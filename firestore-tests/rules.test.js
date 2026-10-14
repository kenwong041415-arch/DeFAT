// Security-rules tests for ../firestore.rules (phase1.5 plan §5.1).
//
// WHY THESE TESTS EXIST: the repository is public and the test APK is published
// login-free, so the Firebase API key is public by design. These rules are the
// ONLY thing protecting the owner's and their students' health data. A suite
// that only shows a user can read their own data proves nothing; most of the
// cases below are NEGATIVE: they show someone else cannot.
//
// Run:  cd firestore-tests && npm ci && npm run test:emulator

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  collectionGroup,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
} from 'firebase/firestore';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..');

/**
 * The subcollection names come from the Kotlin source of truth, so a collection
 * added to SYNCED_SUBCOLLECTIONS is covered by the ownership tests with no edit
 * here. (It is also what the delete-account routine and the sync engine read.)
 */
function syncedSubcollections() {
  const source = readFileSync(
    resolve(repoRoot, 'core-domain/src/main/kotlin/com/defat/core/domain/sync/SyncCollections.kt'),
    'utf8',
  );
  const consts = new Map(
    [...source.matchAll(/const val (\w+) = "([^"]+)"/g)].map((m) => [m[1], m[2]]),
  );
  const list = source.match(/val SYNCED_SUBCOLLECTIONS: List<String> = listOf\(([\s\S]*?)\)\s*\n/);
  assert.ok(list, 'could not find SYNCED_SUBCOLLECTIONS in SyncCollections.kt');
  const names = [...list[1].matchAll(/SyncCollections\.(\w+)/g)].map((m) => {
    assert.ok(consts.has(m[1]), `unresolved constant ${m[1]}`);
    return consts.get(m[1]);
  });
  assert.ok(names.length >= 5, `expected the synced subcollections, got ${JSON.stringify(names)}`);
  return names;
}

const SUBCOLLECTIONS = syncedSubcollections();

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-defat',
    firestore: { rules: readFileSync(resolve(repoRoot, 'firestore.rules'), 'utf8') },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const alice = () => env.authenticatedContext('alice').firestore();
const bob = () => env.authenticatedContext('bob').firestore();
const anonymous = () => env.unauthenticatedContext().firestore();

/** Seeds data as the admin, bypassing rules, so a denial is about the rules and not about missing data. */
async function seed(path, data = { name: 'seeded' }) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), path), data);
  });
}

describe('1. the owner can use their own tree', () => {
  test('a signed-in user reads and writes their own users/{uid}', async () => {
    const db = alice();
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { sex: 'MALE', heightCm: 175 }));
    const snap = await assertSucceeds(getDoc(doc(db, 'users/alice')));
    assert.equal(snap.data().heightCm, 175);
  });
});

describe('2. another signed-in user cannot read it', () => {
  test('bob cannot read users/alice', async () => {
    await seed('users/alice', { sex: 'MALE' });
    await assertFails(getDoc(doc(bob(), 'users/alice')));
  });

  test('bob cannot read a meal under users/alice', async () => {
    await seed('users/alice/meals/m1', { kcal: 500 });
    await assertFails(getDoc(doc(bob(), 'users/alice/meals/m1')));
  });

  test('bob cannot list users/alice/meals', async () => {
    await seed('users/alice/meals/m1', { kcal: 500 });
    await assertFails(getDocs(collection(bob(), 'users/alice/meals')));
  });

  test('bob cannot reach alice through a collection-group query', async () => {
    await seed('users/alice/meals/m1', { kcal: 500 });
    await assertFails(getDocs(query(collectionGroup(bob(), 'meals'))));
  });

  test('bob cannot list the users collection to find alice', async () => {
    await seed('users/alice', { sex: 'MALE' });
    await assertFails(getDocs(collection(bob(), 'users')));
  });
});

describe('3. another signed-in user cannot write it', () => {
  test('bob cannot write users/alice/meals/{id}', async () => {
    await assertFails(setDoc(doc(bob(), 'users/alice/meals/m1'), { kcal: 500 }));
  });

  test('bob cannot overwrite an existing meal of alice', async () => {
    await seed('users/alice/meals/m1', { kcal: 500 });
    await assertFails(setDoc(doc(bob(), 'users/alice/meals/m1'), { kcal: 1 }));
  });

  test('bob cannot delete alice\'s meal or her profile', async () => {
    await seed('users/alice', { sex: 'MALE' });
    await seed('users/alice/meals/m1', { kcal: 500 });
    await assertFails(deleteDoc(doc(bob(), 'users/alice/meals/m1')));
    await assertFails(deleteDoc(doc(bob(), 'users/alice')));
  });
});

describe('4. an unauthenticated client gets nothing', () => {
  test('cannot read users/{uid}', async () => {
    await seed('users/alice', { sex: 'MALE' });
    await assertFails(getDoc(doc(anonymous(), 'users/alice')));
  });

  test('cannot read a subcollection document', async () => {
    await seed('users/alice/weights/2026-03-05', { weightKg: 79.4 });
    await assertFails(getDoc(doc(anonymous(), 'users/alice/weights/2026-03-05')));
  });

  test('cannot write', async () => {
    await assertFails(setDoc(doc(anonymous(), 'users/alice'), { sex: 'MALE' }));
    await assertFails(setDoc(doc(anonymous(), 'users/alice/meals/m1'), { kcal: 1 }));
  });
});

describe('5. the owner can use every subcollection of their own tree', () => {
  for (const name of SUBCOLLECTIONS) {
    test(`reads, writes and deletes users/{uid}/${name}`, async () => {
      const db = alice();
      const ref = doc(db, `users/alice/${name}/d1`);
      await assertSucceeds(setDoc(ref, { updatedAt: 1, n: 'x' }));
      const snap = await assertSucceeds(getDoc(ref));
      assert.equal(snap.data().n, 'x');
      await assertSucceeds(getDocs(collection(db, `users/alice/${name}`)));
      await assertSucceeds(deleteDoc(ref));
    });

    test(`bob is denied users/alice/${name}`, async () => {
      await seed(`users/alice/${name}/d1`, { n: 'x' });
      await assertFails(getDoc(doc(bob(), `users/alice/${name}/d1`)));
      await assertFails(setDoc(doc(bob(), `users/alice/${name}/d2`), { n: 'y' }));
    });
  }

  test('the owner can empty her whole tree (what account deletion does)', async () => {
    await seed('users/alice', { sex: 'MALE' });
    for (const name of SUBCOLLECTIONS) await seed(`users/alice/${name}/d1`);
    const db = alice();
    for (const name of SUBCOLLECTIONS) {
      const snap = await assertSucceeds(getDocs(collection(db, `users/alice/${name}`)));
      for (const d of snap.docs) await assertSucceeds(deleteDoc(d.ref));
    }
    await assertSucceeds(deleteDoc(doc(db, 'users/alice')));
    for (const name of SUBCOLLECTIONS) {
      const after = await getDocs(collection(db, `users/alice/${name}`));
      assert.equal(after.size, 0, `users/alice/${name} not empty`);
    }
  });
});

describe('6. deny by default: nothing outside users/ is writable', () => {
  const outside = [
    'meals/m1',
    'weights/w1',
    'publicLog/entry',
    'config/app',
    'profiles/alice',
    'data/alice',
    'trainers/alice',
  ];
  for (const path of outside) {
    test(`a signed-in user cannot write ${path}`, async () => {
      await assertFails(setDoc(doc(alice(), path), { x: 1 }));
    });
    test(`a signed-in user cannot read ${path}`, async () => {
      await seed(path, { x: 1 });
      await assertFails(getDoc(doc(alice(), path)));
    });
  }

  test('even a path that merely contains the uid is not an ownership grant', async () => {
    await assertFails(setDoc(doc(alice(), 'shared/alice/meals/m1'), { x: 1 }));
    await assertFails(setDoc(doc(alice(), 'backup/users/alice/meals'), { x: 1 }));
  });
});

describe('7. the path decides, not the payload', () => {
  test('alice cannot create users/bob by putting her own uid in the body', async () => {
    await assertFails(
      setDoc(doc(alice(), 'users/bob'), { userId: 'alice', uid: 'alice', owner: 'alice', role: 'trainer' }),
    );
  });

  test('alice cannot create users/bob/meals/{id} claiming ownership in the body', async () => {
    await assertFails(
      setDoc(doc(alice(), 'users/bob/meals/m1'), { userId: 'alice', uid: 'alice', kcal: 1 }),
    );
  });

  test('a body naming someone else does not stop the real owner of the path', async () => {
    // The rule never reads the payload, in either direction.
    await assertSucceeds(setDoc(doc(alice(), 'users/alice/meals/m1'), { userId: 'bob', kcal: 1 }));
  });

  test('bob cannot take over a document alice already owns by setting userId to himself', async () => {
    await seed('users/alice/meals/m1', { userId: 'alice', kcal: 500 });
    await assertFails(setDoc(doc(bob(), 'users/alice/meals/m1'), { userId: 'bob', kcal: 1 }));
  });
});
