package gitlet;
import java.io.Serializable;
import java.io.File;

/**
 * Represents a Gitlet Blob. Each Blob maintains a saved version of a file.
 */
public class Blob implements Serializable {

    /** The file contents of this Blob. */
    private byte[] contents;

    /** Create a new Blob object from a file.
     * @param f A File.
     */
    public Blob(File f) {
        contents = Utils.readContents(f);
    }

    /** Returns the file contents of this Blob. */
    public byte[] getContents() {
        return contents;
    }

    /** Returns the SHA-1 ID of this Blob.
     * @param b A Blob.
     */
    public static String getSha1(Blob b) {
        if (b == null) {
            return "";
        }
        return Utils.sha1(b.getContents());
    }
}
