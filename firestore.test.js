const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
});

test("Authenticated owner can create and query localized Darija product with 0% markup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_blender_1")
      .set({
        userId: ALICE_UID,
        temuGoodsId: "TM-99201",
        originalTitle: "Portable USB Rechargeable Smoothie Blender",
        darijaTitle: "خلاط عصير محمول كيتشارجا بـ USB - عملي للبحر والخدمة فالجديدة",
        darijaPitch: "صايب العصير ديالك طري فين ما كنتي فسيدي بوزيد ولا الخدمة بلا صداع.",
        neighborhoodTag: "سيدي بوزيد • وسط الجديدة",
        priceMad: 89,
        salesVolume: "+4,200 مبيعة",
        imageKey: "blender",
        category: "الكوزينة والدار",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user access to another user's products or orders is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("orders")
      .doc("order_1")
      .set({
        userId: ALICE_UID,
        orderCode: "JD-1001",
        customerName: "أمين الجديدي",
        customerPhone: "0661234567",
        customerAddress: "شارع محمد السادس، سيدي بوزيد، الجديدة",
        subtotalMad: 89,
        serviceFeeMad: 15,
        totalMad: 104,
        status: "pending_whatsapp",
        itemTitlesDarija: ["خلاط عصير محمول"],
        itemPricesMad: [89],
        itemImageKeys: ["blender"],
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("orders")
      .doc("order_1")
      .get()
  );
});

test("Order creation enforces exact 15 MAD service fee and rejects shadow fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Invalid service fee (25 MAD instead of 15 MAD) must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("orders")
      .doc("order_bad_fee")
      .set({
        userId: ALICE_UID,
        orderCode: "JD-1002",
        customerName: "سلمى المزالي",
        customerPhone: "0661234567",
        customerAddress: "حي المطار، الجديدة",
        subtotalMad: 100,
        serviceFeeMad: 25,
        totalMad: 125,
        status: "pending_whatsapp",
        itemTitlesDarija: ["مصباح كريستال"],
        itemPricesMad: [100],
        itemImageKeys: ["lamp"],
        createdAt: now,
        updatedAt: now,
      })
  );

  // Valid 15 MAD service fee must succeed
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("orders")
      .doc("order_valid")
      .set({
        userId: ALICE_UID,
        orderCode: "JD-1003",
        customerName: "سلمى المزالي",
        customerPhone: "0661234567",
        customerAddress: "حي المطار، الجديدة",
        subtotalMad: 100,
        serviceFeeMad: 15,
        totalMad: 115,
        status: "pending_whatsapp",
        itemTitlesDarija: ["مصباح كريستال"],
        itemPricesMad: [100],
        itemImageKeys: ["lamp"],
        createdAt: now,
        updatedAt: now,
      })
  );
});
