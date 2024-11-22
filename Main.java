package gitlet;

import java.io.File;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.text.SimpleDateFormat;

/** Driver class for Gitlet, a subset of the Git version-control system.
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                gitletInit(args);
                break;
            case "add":
                gitletAdd(args);
                break;
            case "commit":
                gitletCommit(args, null);
                break;
            case "rm":
                gitletRemove(args);
                break;
            case "log":
                gitletLog(args);
                break;
            case "global-log":
                gitletGlobalLog(args);
                break;
            case "find":
                gitletFind(args);
                break;
            case "status":
                gitletStatus(args);
                break;
            case "checkout":
                gitletCheckout(args);
                break;
            case "branch":
                gitletBranch(args);
                break;
            case "rm-branch":
                gitletRemoveBranch(args);
                break;
            case "reset":
                gitletReset(args);
                break;
            case "merge":
                gitletMerge(args);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
    }

    /** Initializes a Gitlet repository. */
    public static void gitletInit(String[] args) {
        validateNumArgs(args, 1);
        if (Repository.GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system already exists in the current "
                    + "directory.");
        }

        Repository.GITLET_DIR.mkdir();
        Repository.STAGING_ADDITION.mkdir();
        Repository.STAGING_DELETION.mkdir();
        Repository.BLOBS.mkdir();
        Repository.COMMITS.mkdir();
        Repository.BRANCHES.mkdir();
        File head = new File(Repository.GITLET_DIR, "HEAD");
        Utils.writeContents(head, "");

        Commit initialCommit = new Commit("initial commit", new Date(0), null, null, null);
        Branch initialBranch = new Branch(initialCommit, "main", true);
        Repository.saveBranch(initialBranch);
        Utils.writeObject(head, initialBranch);
        Repository.saveCommit(initialCommit);
    }

    public static void gitletAdd(String[] args) {
        checkIfInitialized();
        validateNumArgs(args, 2);
        File f = Utils.join(CWD, args[1]);
        if (!f.exists()) {
            exitWithError("File does not exist.");
        }

        File fRemoval = Utils.join(Repository.STAGING_DELETION, args[1]);
        if (fRemoval.exists()) {
            fRemoval.delete();
        }

        File fAdd = Utils.join(Repository.STAGING_ADDITION, args[1]);
        Commit c = Repository.getCurrentCommit();
        if (c.getMap().containsKey(f.getName())) {
            String s = c.getMap().get(f.getName());
            if (Arrays.equals(Repository.loadBlob(s).getContents(), Utils.readContents(f))) {
                fAdd.delete();
                return;
            }
        }

        Utils.writeContents(fAdd, Utils.readContents(f));
    }

    public static void gitletCommit(String[] args, Commit parent2) {
        checkIfInitialized();
        validateNumArgs(args, 2);

        if (args[1].equals("")) {
            exitWithError("Please enter a commit message.");
        }

        if (Utils.plainFilenamesIn(Repository.STAGING_ADDITION).isEmpty()
                && Utils.plainFilenamesIn(Repository.STAGING_DELETION).isEmpty()) {
            exitWithError("No changes added to the commit.");
        }

        Commit c = Repository.getCurrentCommit();
        HashMap<String, Blob> blobs = new HashMap<String, Blob>();
        ArrayList<String> fileNames = new ArrayList<String>();
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_ADDITION)) {
            File f = Utils.join(Repository.STAGING_ADDITION, s);
            Blob newBlob = new Blob(f);
            blobs.put(s, newBlob);
            fileNames.add(s);
        }
        for (String fileName : c.getMap().keySet()) {
            String s = c.getMap().get(fileName);
            Blob b = Repository.loadBlob(s);
            if (!fileNames.contains(fileName)
                    && !Utils.plainFilenamesIn(Repository.STAGING_DELETION).contains(fileName)) {
                blobs.put(fileName, b);
                fileNames.add(fileName);
            }
        }

        Commit newCommit = new Commit(args[1], new Date(), c, blobs, parent2);
        Repository.saveCommit(newCommit);
        for (Blob b : blobs.values()) {
            Repository.saveBlob(b);
        }
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_ADDITION)) {
            File f = Utils.join(Repository.STAGING_ADDITION, s);
            f.delete();
        }
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_DELETION)) {
            File f = Utils.join(Repository.STAGING_DELETION, s);
            f.delete();
        }
    }

    public static void gitletRemove(String[] args) {
        checkIfInitialized();
        validateNumArgs(args, 2);
        if (!Utils.plainFilenamesIn(Repository.STAGING_ADDITION).contains(args[1])) {
            if (!Repository.getCurrentCommit().getMap().containsKey(args[1])) {
                exitWithError("No reason to remove the file.");
            }
        }

        File fRemoval = Utils.join(Repository.STAGING_ADDITION, args[1]);
        if (fRemoval.exists()) {
            fRemoval.delete();
        }

        File fAdd = Utils.join(Repository.STAGING_DELETION, args[1]);
        Utils.writeContents(fAdd, "");

        File f = Utils.join(CWD, args[1]);
        if (f.exists() && Repository.getCurrentCommit().getMap().containsKey(args[1])) {
            f.delete();
        }
    }

    public static void gitletLog(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 1);
        Commit c = Repository.getCurrentCommit();
        while (true) {
            System.out.println("===");
            System.out.println("commit " + Utils.sha1(c.toString()));
            if (c.getIsMerge()) {
                System.out.println("Merge: " + c.getParent().substring(0, 7)
                        + " " + c.getParent2().substring(0, 7));
            }
            SimpleDateFormat d = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            System.out.println("Date: " + d.format(c.getTime()));
            System.out.println(c.getMessage());
            System.out.println();

            if (c.getParent().equals("")) {
                break;
            }
            c = Repository.loadCommit(c.getParent());
        }
    }

    public static void gitletGlobalLog(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 1);
        for (String s : Utils.plainFilenamesIn(Repository.COMMITS)) {
            Commit c = Repository.loadCommit(s);
            System.out.println("===");
            System.out.println("commit " + Utils.sha1(c.toString()));
            if (c.getIsMerge()) {
                System.out.println("Merge: " + c.getParent().substring(0, 7)
                        + " " + c.getParent2().substring(0, 7));
            }
            SimpleDateFormat d = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            System.out.println("Date: " + d.format(c.getTime()));
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    public static void gitletFind(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 2);
        String toFind = args[1];
        int counter = 0;
        for (String s : Utils.plainFilenamesIn(Repository.COMMITS)) {
            Commit c = Repository.loadCommit(s);
            if (c.getMessage().equals(toFind)) {
                System.out.println(Commit.getSha1(c));
                counter++;
            }
        }
        if (counter == 0) {
            exitWithError("Found no commit with that message.");
        }
    }

    public static void gitletStatus(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 1);

        File head = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch headBranch = Utils.readObject(head, Branch.class);
        System.out.println("=== Branches ===");
        for (String s : Utils.plainFilenamesIn(Repository.BRANCHES)) {
            Branch b = Repository.loadBranch(s);
            if (b.getName().equals(headBranch.getName())) {
                System.out.println("*" + b.getName());
            } else {
                System.out.println(b.getName());
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_ADDITION)) {
            System.out.println(s);
        }
        System.out.println();

        Commit headCommit = headBranch.getCommit();
        System.out.println("=== Removed Files ===");
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_DELETION)) {
            if (headCommit.getMap().containsKey(s)) {
                System.out.println(s);
            }
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void gitletCheckout(String[] args) {
        checkIfInitialized();

        if (args.length == 3 && args[1].equals("--")) {
            Commit c = Repository.getCurrentCommit();
            String fileName = args[2];
            if (!c.getMap().containsKey(fileName)) {
                exitWithError("File does not exist in that commit.");
            }
            Blob b = Repository.loadBlob(c.getMap().get(fileName));
            File f = Utils.join(CWD, fileName);
            Utils.writeContents(f, b.getContents());
            return;
        }

        if (args.length == 4 && args[2].equals("--")) {
            String s = args[1];
            boolean flag = false;
            for (String id : Utils.plainFilenamesIn(Repository.COMMITS)) {
                if (id.startsWith(s)) {
                    s = id;
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                exitWithError("No commit with that id exists.");
            }

            Commit c = Repository.loadCommit(s);
            String fileName = args[3];
            if (!c.getMap().containsKey(fileName)) {
                exitWithError("File does not exist in that commit.");
            }
            Blob b = Repository.loadBlob(c.getMap().get(fileName));
            File f = Utils.join(CWD, fileName);
            Utils.writeContents(f, b.getContents());
            return;
        }

        if (args.length == 2) {
            gitletCheckoutCase3(args);
            return;
        }

        exitWithError("Incorrect operands.");
    }

    private static void gitletCheckoutCase3(String[] args) {
        File f = Utils.join(Repository.BRANCHES, args[1]);
        if (!f.exists()) {
            exitWithError("No such branch exists.");
        }

        File h = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(h, Branch.class);
        if (b.getName().equals(args[1])) {
            exitWithError("No need to checkout the current branch.");
        }

        Commit currentCommit = b.getCommit();
        Branch checkoutBranch = Repository.loadBranch(args[1]);
        Commit checkoutCommit = checkoutBranch.getCommit();
        for (String s : Utils.plainFilenamesIn(CWD)) {
            File file2 = Utils.join(Repository.STAGING_ADDITION, s);
            boolean tracked1 = file2.exists();
            boolean tracked2 = currentCommit.getMap().keySet().contains(s);
            boolean tracked = tracked1 || tracked2;
            if (!tracked) {
                exitWithError("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        for (String s : Utils.plainFilenamesIn(CWD)) {
            File file = Utils.join(CWD, s);
            File file2 = Utils.join(Repository.STAGING_ADDITION, s);
            boolean tracked1 = file2.exists();
            boolean tracked2 = currentCommit.getMap().keySet().contains(s);
            boolean tracked = tracked1 || tracked2;
            if (tracked) {
                file.delete();
            }
        }

        Utils.writeObject(h, checkoutBranch);
        Branch currentBranchChanged = new Branch(currentCommit,
                b.getName(), false);
        Branch checkoutBranchChanged = new Branch(checkoutCommit,
                checkoutBranch.getName(), true);
        Repository.saveBranch(currentBranchChanged);
        Repository.saveBranch(checkoutBranchChanged);
        for (String s : checkoutCommit.getMap().keySet()) {
            gitletCheckout(new String[]{"checkout", Commit.getSha1(checkoutCommit), "--", s});
        }

        for (String s : Utils.plainFilenamesIn(Repository.STAGING_ADDITION)) {
            File fRemove = Utils.join(Repository.STAGING_ADDITION, s);
            fRemove.delete();
        }
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_DELETION)) {
            File fRemove = Utils.join(Repository.STAGING_DELETION, s);
            fRemove.delete();
        }
    }

    public static void gitletBranch(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 2);
        File f = Utils.join(Repository.BRANCHES, args[1]);
        if (f.exists()) {
            exitWithError("A branch with that name already exists.");
        }

        Branch b = new Branch(Repository.getCurrentCommit(), args[1], false);
        Repository.saveBranch(b);
    }

    public static void gitletRemoveBranch(String[] args) {
        checkIfInitialized();

        validateNumArgs(args, 2);
        File f = Utils.join(Repository.BRANCHES, args[1]);
        if (!f.exists()) {
            exitWithError("A branch with that name does not exist.");
        }

        File h = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(h, Branch.class);
        if (b.getName().equals(args[1])) {
            exitWithError("Cannot remove the current branch.");
        }

        f.delete();
    }

    public static void gitletReset(String[] args) {
        checkIfInitialized();
        validateNumArgs(args, 2);

        String commitName = args[1];
        boolean flag = false;
        for (String id : Utils.plainFilenamesIn(Repository.COMMITS)) {
            if (id.startsWith(commitName)) {
                commitName = id;
                flag = true;
                break;
            }
        }
        if (!flag) {
            exitWithError("No commit with that id exists.");
        }

        File h = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(h, Branch.class);
        Commit currentCommit = b.getCommit();
        Commit checkoutCommit = Repository.loadCommit(commitName);
        for (String s : Utils.plainFilenamesIn(CWD)) {
            File file2 = Utils.join(Repository.STAGING_ADDITION, s);
            boolean tracked1 = file2.exists();
            boolean tracked2 = currentCommit.getMap().keySet().contains(s);
            boolean tracked = tracked1 || tracked2;
            if (!tracked) {
                exitWithError("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        for (String s : Utils.plainFilenamesIn(CWD)) {
            File file = Utils.join(CWD, s);
            File file2 = Utils.join(Repository.STAGING_ADDITION, s);
            boolean tracked1 = file2.exists();
            boolean tracked2 = currentCommit.getMap().keySet().contains(s);
            boolean tracked = tracked1 || tracked2;
            if (tracked) {
                file.delete();
            }
        }

        Branch changedBranch = new Branch(checkoutCommit, b.getName(), true);
        Utils.writeObject(h, changedBranch);
        Repository.saveBranch(changedBranch);
        for (String s : checkoutCommit.getMap().keySet()) {
            gitletCheckout(new String[]{"checkout", Commit.getSha1(checkoutCommit), "--", s});
        }

        for (String s : Utils.plainFilenamesIn(Repository.STAGING_ADDITION)) {
            File fRemove = Utils.join(Repository.STAGING_ADDITION, s);
            fRemove.delete();
        }
        for (String s : Utils.plainFilenamesIn(Repository.STAGING_DELETION)) {
            File fRemove = Utils.join(Repository.STAGING_DELETION, s);
            fRemove.delete();
        }
        return;
    }

    public static void gitletMerge(String[] args) {
        mergeCheckErrors(args);
        File h = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(h, Branch.class);
        Commit head = b.getCommit();
        Commit other = Repository.loadBranch(args[1]).getCommit();
        Commit split = Repository.latestAncestor(head, other);
        if (Commit.getSha1(head).equals(Commit.getSha1(split))) {
            gitletCheckout(new String[]{"checkout", Repository.loadBranch(args[1]).getName()});
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        boolean conflict = false;
        for (String s : split.getMap().keySet()) {
            Blob splitBlob = Repository.loadBlob(split.getMap().get(s));
            Blob headBlob = null;
            if (head.getMap().containsKey(s)) {
                headBlob = Repository.loadBlob(head.getMap().get(s));
            }
            Blob otherBlob = null;
            if (other.getMap().containsKey(s)) {
                otherBlob = Repository.loadBlob(other.getMap().get(s));
            }
            if (Blob.getSha1(splitBlob).equals(Blob.getSha1(headBlob))) {
                if (!Blob.getSha1(splitBlob).equals(Blob.getSha1(otherBlob))) {
                    if (otherBlob == null) {
                        gitletRemove(new String[]{"remove", s});
                    } else {
                        gitletCheckout(new String[]{"checkout", Commit.getSha1(other), "--", s});
                        gitletAdd(new String[]{"add", s});
                    }
                    continue;
                }
            }
            if (!Blob.getSha1(splitBlob).equals(Blob.getSha1(headBlob))) {
                if (!Blob.getSha1(splitBlob).equals(Blob.getSha1(otherBlob))) {
                    if (!Blob.getSha1(headBlob).equals(Blob.getSha1(otherBlob))) {
                        String currContents;
                        if (headBlob != null) {
                            currContents = new String(headBlob.getContents());
                        } else {
                            currContents = "";
                        }
                        String otherContents;
                        if (otherBlob != null) {
                            otherContents = new String(otherBlob.getContents());
                        } else {
                            otherContents = "";
                        }
                        File mergeConflict = Utils.join(CWD, s);
                        Utils.writeContents(mergeConflict, "<<<<<<< HEAD\n" + currContents
                                + "=======\n" + otherContents + ">>>>>>>" + "\n");
                        gitletAdd(new String[]{"add", s});
                        conflict = true;
                    }
                }
            }
        }
        for (String s : other.getMap().keySet()) {
            Blob otherBlob = Repository.loadBlob(other.getMap().get(s));
            Blob splitBlob = null;
            if (split.getMap().containsKey(s)) {
                splitBlob = Repository.loadBlob(split.getMap().get(s));
            }
            Blob headBlob = null;
            if (head.getMap().containsKey(s)) {
                headBlob = Repository.loadBlob(head.getMap().get(s));
            }
            if (headBlob == null && splitBlob == null) {
                File newFile = Utils.join(CWD, s);
                Utils.writeContents(newFile, otherBlob.getContents());
                gitletAdd(new String[]{"add", s});
            }
        }
        gitletCommit(new String[]{"commit", "Merged " + args[1] + " into " + b.getName() + "."},
                other);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static void mergeCheckErrors(String[] args) {
        checkIfInitialized();
        validateNumArgs(args, 2);
        File h = Utils.join(Repository.GITLET_DIR, "HEAD");
        Branch b = Utils.readObject(h, Branch.class);
        Commit currentCommit = b.getCommit();
        for (String s : Utils.plainFilenamesIn(CWD)) {
            File file2 = Utils.join(Repository.STAGING_ADDITION, s);
            boolean tracked1 = file2.exists();
            boolean tracked2 = currentCommit.getMap().keySet().contains(s);
            boolean tracked = tracked1 || tracked2;
            if (!tracked) {
                exitWithError("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        if (!Utils.plainFilenamesIn(Repository.STAGING_ADDITION).isEmpty()
                || !Utils.plainFilenamesIn(Repository.STAGING_DELETION).isEmpty()) {
            exitWithError("You have uncommitted changes.");
        }
        File f = Utils.join(Repository.BRANCHES, args[1]);
        if (!f.exists()) {
            exitWithError("A branch with that name does not exist.");
        }
        if (b.getName().equals(args[1])) {
            exitWithError("Cannot merge a branch with itself.");
        }
        Commit head = b.getCommit();
        Commit other = Repository.loadBranch(args[1]).getCommit();
        Commit split = Repository.latestAncestor(head, other);
        if (Commit.getSha1(other).equals(Commit.getSha1(split))) {
            exitWithError("Given branch is an ancestor of the current branch.");
        }
    }

    public static void exitWithError(String message) {
        System.out.println(message);
        System.exit(0);
    }

    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exitWithError("Incorrect operands.");
        }
    }

    public static void checkIfInitialized() {
        if (!Repository.GITLET_DIR.exists()) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
    }
}
