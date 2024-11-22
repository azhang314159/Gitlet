package gitlet;

import java.io.Serializable;

/**
 * A class for representing a Gitlet Branch.
 */
public class Branch implements Serializable {
    /**
     * The head commit of this branch.
     */
    private Commit head;

    /**
     * Whether this branch is the HEAD commit of the repository or not.
     */
    private boolean isHead;

    /**
     * The name of this branch.
     */
    private String name;

    /**
     * Create a new Branch object.
     * @param c A Commit
     * @param name The name of this Branch
     * @param isHead Whether this Branch is the HEAD commit or not
     */
    public Branch(Commit c, String name, boolean isHead) {
        head = c;
        this.name = name;
        this.isHead = isHead;
    }

    /**
     * Returns a String representation of this Branch.
     */
    @Override
    public String toString() {
        return head.toString() + " " + name + " " + isHead;
    }

    /**
     * Returns the name of this branch.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the head Commit of this branch.
     */
    public Commit getCommit() {
        return head;
    }
}
