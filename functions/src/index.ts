import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";

admin.initializeApp();

/**
 * Callable function that deletes a user from Firebase Authentication.
 *
 * Must be called by an admin user. Verifies the caller has the "admin" role
 * by checking their Firestore user document before proceeding.
 *
 * Expects: { userId: string } — the UID of the user to delete from Auth.
 * Returns: { success: true } on success.
 * Throws: HttpsError on auth/permission/validation failures.
 */
export const deleteUserAuth = onCall(async (request) => {
  // Require authentication
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Must be authenticated to call this function."
    );
  }

  const callerUid = request.auth.uid;

  // Verify caller is an admin by checking their Firestore user doc
  const callerDoc = await admin.firestore()
    .collection("users")
    .doc(callerUid)
    .get();

  if (!callerDoc.exists) {
    throw new HttpsError(
      "permission-denied",
      "Caller user document not found."
    );
  }

  const callerRole = callerDoc.data()?.role;
  if (callerRole !== "admin") {
    throw new HttpsError(
      "permission-denied",
      "Only admins can delete users from Authentication."
    );
  }

  // Validate input
  const targetUserId = request.data?.userId;
  if (!targetUserId || typeof targetUserId !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Must provide a valid userId string."
    );
  }

  // Prevent admin from deleting themselves
  if (targetUserId === callerUid) {
    throw new HttpsError(
      "invalid-argument",
      "Cannot delete your own account via admin function."
    );
  }

  // Delete the user from Firebase Auth
  try {
    await admin.auth().deleteUser(targetUserId);
    return {success: true};
  } catch (error: unknown) {
    const authError = error as {code?: string; message?: string};
    if (authError.code === "auth/user-not-found") {
      // User already deleted from Auth — consider this a success
      return {success: true};
    }
    throw new HttpsError(
      "internal",
      `Failed to delete user from Auth: ${authError.message || "Unknown error"}`
    );
  }
});
