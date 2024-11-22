package gitlet;

import java.io.File;
import java.util.Queue;
import java.util.LinkedList;

import static gitlet.Utils.*;

/** Represents a Gitlet Repository, which maintains the necessary files and directories for Gitlet.
 *  There are static utility methods for loading and saving Commits, Blobs, and Branches.
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The staging area for files to be added. */
    public static final File STAGING_ADDITION = join(GITLET_DIR, "staging_addition");

    /** The staging area for files to be deleted. */
    public static final File STAGING_DELETION = join(GITLET_DIR, "staging_deletion");

    /** The directory for commits. */
    public static final File COMMITS = join(GITLET_DIR, "commits");

    /** The directory for blobs. */
    public static final File BLOBS = join(GITLET_DIR, "blobs");

    /** The directory for branches. */
    public static final File BRANCHES = join(GITLET_DIR, "branches");

    /** Saves a Commit c to the commits directory, and updates branches accordingly. */
    public static void saveCommit(Commit c) {
        File f = Utils.join(COMMITS, Commit.getSha1(c));
        Utils.writeObject(f, c);

        File head = Utils.join(GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(head, Branch.class);
        b = new Branch(c, b.getName(), true);
        Utils.writeObject(head, b);
        File branchFile = new File(BRANCHES, b.getName());
        Utils.writeObject(branchFile, b);
    }

    /** Load a Commit object from a given file name. */
    public static Commit loadCommit(String name) {
        File f = Utils.join(COMMITS, name);
        if (!f.exists()) {
            return null;
        }
        try {
            Commit commit = Utils.readObject(f, Commit.class);
            return commit;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Saves a Blob b to the blobs directory. */
    public static void saveBlob(Blob b) {
        File f = Utils.join(BLOBS, Blob.getSha1(b));
        Utils.writeObject(f, b);
    }

    /** Load a Blob object from a given file name. */
    public static Blob loadBlob(String name) {
        File f = Utils.join(BLOBS, name);
        if (!f.exists()) {
            return null;
        }
        try {
            Blob blob = Utils.readObject(f, Blob.class);
            return blob;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Saves a branch b to the branches directory. */
    public static void saveBranch(Branch b) {
        File f = Utils.join(BRANCHES, b.getName());
        Utils.writeObject(f, b);
    }

    /** Load a Branch object from a given file name. */
    public static Branch loadBranch(String name) {
        File f = Utils.join(BRANCHES, name);
        if (!f.exists()) {
            return null;
        }
        try {
            Branch branch = Utils.readObject(f, Branch.class);
            return branch;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Loads the HEAD commit. */
    public static Commit getCurrentCommit() {
        File head = Utils.join(GITLET_DIR, "HEAD");
        Branch headBranch = Utils.readObject(head, Branch.class);
        return headBranch.getCommit();
    }

    /**
     * Helper method for gitletMerge. Determines the latest common ancestor of Commits A, B.
     * For this algorithm, we perform a Breadth-First Search.
     */
    public static Commit latestAncestor(Commit a, Commit b) {
        Queue<Commit> fringe = new LinkedList<>();
        fringe.add(a);
        while (!fringe.isEmpty()) {
            Commit node = fringe.remove();
            if (isAncestor(node, b)) {
                return node;
            }
            if (node.getParent().equals("")) {
                return node;
            }

            Commit next = Repository.loadCommit(node.getParent());
            fringe.add(next);
            if (node.getIsMerge()) {
                Commit next2 = Repository.loadCommit(node.getParent2());
                fringe.add(next2);
            }
        }

        return null;
    }

    /** Private helper method for determining if Commit A is an ancestor of Commit B. */
    private static boolean isAncestor(Commit a, Commit b) {
        if (a.getParent().equals("")) {
            return true;
        }
        if (Commit.getSha1(b).equals(Commit.getSha1(a))) {
            return true;
        }
        if (!b.getParent().equals("")) {
            boolean mergeParentAncestor = false;
            if (b.getIsMerge()) {
                Commit b2 = loadCommit(b.getParent2());
                mergeParentAncestor = isAncestor(a, b2);
            }
            b = loadCommit(b.getParent());
            return isAncestor(a, b) || mergeParentAncestor;
        }

        return false;
    }
}
