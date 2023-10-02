package code.commit.gitlab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 *  The change list of this commit, which including updating, creation or deletion status.
 *  
 * @author linzhw
 * @version 1.0
 *
 */
@Slf4j
public class SourceFilesChangeListBuilder {
    
    private List<String> deletions = new ArrayList<>(0);
    private Map<String, Path> updates = new HashMap<>();

    public static SourceFilesChangeListBuilder create() {
        return new SourceFilesChangeListBuilder();
    }

    public String buildGraphString() {
        StringBuilder sb = new StringBuilder();
        if (!this.deletions.isEmpty()) {
            sb.append("deletions:[");
            this.deletions.forEach(item -> {
                sb.append(String.format("{path:\"%s\"}", item));
            });
            sb.append("]");
        }
        if (!this.updates.isEmpty()) {
            sb.append("additions:[");
            this.updates.entrySet().forEach(item -> {
                sb.append(String.format("{path:\"%s\", contents:\"%s\"}", item.getKey(),
                        readFileContentWithBase64(item.getValue())));
            });
            sb.append("]");
        }
        return sb.toString();
    }

    public long getTotalSizeOfUpdateFiles() {
        return this.updates.values().stream().map(t -> {
            try {
                return Files.size(t);
            } catch (IOException e) {
                log.warn("Failed to calculate file size.", e);
            }
            return 0L;
        }).reduce(0L, (a,b) -> a+b);
    }

    public String getChangeList() {
        StringBuilder sb = new StringBuilder();
        if (!this.deletions.isEmpty()) {
            sb.append("- deletions: \n");
            this.deletions.forEach(item -> {
                sb.append(String.format("  - %s\n", item));
            });
        }
        if (!this.updates.isEmpty()) {
            sb.append("- updates: \n");
            this.updates.entrySet().forEach(item -> {
                sb.append(String.format("  - %s\n", item.getKey()));
            });
        }
        return sb.toString();
    }

    /**
     * Read file with base64 encoding as String
     * 
     * @param filePath
     *            the path of reading file
     * @return the base64 encoded string
     * @throws IOException
     */
    private String readFileContentWithBase64(Path filePath) {
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new GithubRemoteRepositoryOperationException("Failed to read file: " + filePath);
        }
        return Base64.getEncoder().encodeToString(fileData);
    }

    /**
     * The path relative to root path of current repository
     * <p>
     * e.g: <br>
     * full path: <b>
     * https://github.com/uu/demo/blob/test/perspiciatis/maiores.txt</b>
     * <br>
     * ==> relative path: <b>perspiciatis/maiores.txt</b>
     * 
     * @param relativePath
     *            the relative path
     * @return current builder
     */
    public SourceFilesChangeListBuilder addDeleteFile(String relativePath) {
        Assert.hasLength(relativePath, "The relative path must not be empty!");
        this.deletions.add(relativePath);
        return this;
    }

    /**
     * Add a create or update file
     * 
     * @param filePath
     *            the update file which must be exist.
     * @param relativePath
     *            the relative path of update file.
     * @return current builder
     */
    public SourceFilesChangeListBuilder addUpdateFile(Path filePath, String relativePath) {
        Assert.hasLength(relativePath, "The relative path must not be empty!");
        Assert.notNull(filePath, "The update file must not be null");
        Assert.isTrue(filePath.toFile().exists(), "The update file '" + filePath + "' does not exist!");
        this.updates.put(relativePath, filePath);
        return this;
    }
}
