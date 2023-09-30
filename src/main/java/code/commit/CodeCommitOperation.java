package code.commit;

import code.commit.gitlab.CommitInput;

/**
 *
 * @author linzhw
 * @version 1.0
 *
 */
public interface CodeCommitOperation {
    void doAction(CommitInput commitInput);
}