package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/** Represents a Gitlet Commit object.
 *  Each Commit saves a "snapshot" of files in the working directory.
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;

    /** The timestamp of this Commit. */
    private Date timestamp;

    /** The SHA-1 ID of the parent Commit. */
    private String parent;

    /** The SHA-1 ID of the merge parent Commit, if it exists. */
    private String parent2;

    /** Whether this Commit is a merge or not. */
    private boolean isMerge;

    /** A Map that maps names of files in this Commit to Blob SHA-1 IDs. */
    private HashMap<String, String> blobsMap;

    public Commit(String message, Date timestamp, Commit parent, HashMap<String,
            Blob> blobs, Commit parent2) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = Commit.getSha1(parent);

        blobsMap = new HashMap<>();
        if (blobs != null) {
            for (String s : blobs.keySet()) {
                blobsMap.put(s, Blob.getSha1(blobs.get(s)));
            }
        }

        this.parent2 = Commit.getSha1(parent2);
        if (parent2 != null) {
            isMerge = true;
        }
    }

    public String getMessage() {
        return message;
    }

    public Date getTime() {
        return timestamp;
    }

    public String getParent() {
        return parent;
    }

    public String getParent2() {
        return parent2;
    }

    public boolean getIsMerge() {
        return isMerge;
    }

    public HashMap<String, String> getMap() {
        return blobsMap;
    }

    public String toString() {
        String blobs = "";
        for (String s : blobsMap.values()) {
            blobs += s + " ";
        }
        return message + " " + timestamp.toString() + " " + parent + " " + parent2 + " " + blobs;
    }

    public static String getSha1(Commit c) {
        if (c == null) {
            return "";
        }
        return Utils.sha1(c.toString());
    }
}
