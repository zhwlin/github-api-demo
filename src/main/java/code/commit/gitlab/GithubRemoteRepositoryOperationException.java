package code.commit.gitlab;

/**
 *
 * @author linzhw
 * @version 1.0
 *
 */
public class GithubRemoteRepositoryOperationException extends RuntimeException {
    public GithubRemoteRepositoryOperationException(String message) {
        super(message);
    }
    public GithubRemoteRepositoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}