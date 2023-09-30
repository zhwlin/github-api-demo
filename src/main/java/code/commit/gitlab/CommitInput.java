package code.commit.gitlab;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author linzhw
 * @version 1.0
 *
 */
@Accessors(chain = true)
@Data
public class CommitInput {

    String owner;
    String userToken;
    String repoName;
    String branch;
    String commitMsgHeader;
    String commitMsgBody;
    SourceFilesChangeListBuilder changeList;
}
