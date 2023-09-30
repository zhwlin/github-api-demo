package code.commit.gitlab;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;

import code.commit.CodeCommitOperation;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author linzhw
 * @version 1.0
 *
 */
@Slf4j
public class GithubCommitOperation implements CodeCommitOperation {

    /**
     * 
     */
    private static final String API_GITHUB_GRAPHQL = "https://api.github.com/graphql";
    private static final Pattern REG_OID = Pattern.compile("\"oid\":\"(.*?)\"");
    private static final String REQ_OID_QRY_TEMPLATE = getOidQueryTemplate();
    private static final String REQ_COMMIT_OP_TEMPLATE = getCommitOpMutationTemplate();
    RestTemplate client;

    /**
     * 
     */
    public GithubCommitOperation() {
        client = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(60)).build();
    }
    
    public void doAction(CommitInput commitInput) {
        String lastestCommitId = this.getLastestCommitId(commitInput.getOwner(), 
                                                         commitInput.getRepoName(),
                                                         commitInput.getUserToken(),
                                                         commitInput.getBranch()
                                                         );
        if(log.isDebugEnabled()) {
           log.debug("Last commit-id:{}, owner:{}, repos:{}, branch:{}", lastestCommitId,
                                                           commitInput.getOwner(),
                                                           commitInput.getRepoName(),
                                                           commitInput.getBranch()
                     ); 
        }
        String repoNameWithOwner = commitInput.getOwner() + "/" + commitInput.getRepoName();
        this.doCommitOp(commitInput.getUserToken(), 
                        repoNameWithOwner, 
                        commitInput.getBranch(),
                        lastestCommitId,
                        commitInput.getCommitMsgHeader(),
                        commitInput.getCommitMsgBody(),
                        commitInput.getChangeList()
                        );
        if(log.isDebugEnabled()) {
            long fileSize = commitInput.getChangeList().getTotalSizeOfUpdateFiles();
            String size = DataSize.ofBytes(fileSize).toKilobytes() + "KB";
            log.debug("Succeed to commit! Change list({}): {}", size, commitInput.getChangeList().getChangeList());
        }
        
    }

    private void doCommitOp(String userToken,
                            String repoNameWithowner, 
                            String branch, 
                            String lastestOid,
                            String commitMessageHeadline, 
                            String commitMessageBody, 
                            SourceFilesChangeListBuilder changes) {
        String queryBody = this.getCommitOpMutationQuery(repoNameWithowner,
                                                        branch,
                                                        lastestOid,
                                                        commitMessageHeadline,
                                                        commitMessageBody,
                                                        changes);
        if(log.isDebugEnabled()) {
            log.debug("Commit query: {}", queryBody);
        }
        String r = doGithubGraphqlRequestWithBody(userToken, queryBody);
        if (r.indexOf("\"errors\"") > -1) {
            Pattern p = Pattern.compile("\"message\":\"(.*?)\"");
            Optional<String> m = findFirstGroupByReg(r, p);
            throw new GithubRemoteRepositoryOperationException(
                    "Failed to commit to github, reason: " + m.orElse("unknown reason"));
        }
        if(log.isDebugEnabled()) {
            log.debug("Result: {}", r);
        }
    }

    private String getLastestCommitId(String owner, String repoName, String userToken, String branch) {
        String body = getOidQuery(owner, repoName, branch);
        String r = doGithubGraphqlRequestWithBody(userToken, body);
        Optional<String> m = findFirstGroupByReg(r, REG_OID);
        return m.orElseThrow(() -> {
            return new GithubRemoteRepositoryOperationException(String.format(
                    "Failed to get lastest commit id on this branch:%s, owner:%s, repo:%s!", branch, owner, repoName));
        });
    }

    private Optional<String> findFirstGroupByReg(String r, Pattern p) {
        Matcher m = p.matcher(r);
        if (m.find()) {
            return Optional.ofNullable(m.group(1));
        }
        return Optional.empty();
    }

    /**
     * @param userToken
     * @param body
     * @return
     */
    private String doGithubGraphqlRequestWithBody(String userToken, String body) {
        return post(API_GITHUB_GRAPHQL, userToken, formatRequestBody(body));
    }

    private String getCommitOpMutationQuery(String repoNameWithowner, String branch, String lastestOid,
            String commitMessageHeadline, String commitMessageBody, SourceFilesChangeListBuilder changes) {
        return String.format(REQ_COMMIT_OP_TEMPLATE, escapeInput(repoNameWithowner), escapeInput(branch),
                escapeInputMessage(commitMessageHeadline), escapeInputMessage(commitMessageBody),
                escapeInput(lastestOid), escapeInput(changes.buildGraphString()));
    }

    private static String getCommitOpMutationTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("mutation  {");
        sb.append("  createCommitOnBranch( input: {");
        sb.append("         branch: {");
        sb.append("          repositoryNameWithOwner: \"%s\",");
        sb.append("          branchName: \"%s\" ");
        sb.append("        }, ");
        sb.append("        message: {");
        sb.append("            headline: \"%s\"");
        sb.append("            body: \"%s\"");
        sb.append("         }, ");
        sb.append("        expectedHeadOid: \"%s\", ");
        sb.append("        fileChanges: {%s}");
        sb.append("    }) {");
        sb.append("    commit {");
        sb.append("      url");
        sb.append("    }");
        sb.append("  }");
        sb.append("}");
        return sb.toString().replaceAll("  ", "");
    }

    /**
     * @param owner
     * @param repoName
     * @param branch
     * @return
     */
    private String getOidQuery(String owner, String repoName, String branch) {
        return String.format(REQ_OID_QRY_TEMPLATE, escapeInput(owner), escapeInput(repoName), escapeInput(branch));
    }

    private static String getOidQueryTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("query {");
        sb.append("repository(owner: \"%s\", name: \"%s\") {                ");
        sb.append("  refs(refPrefix: \"refs/heads/\", first: 1, query: \"%s\") {");
        sb.append("    edges {                                                 ");
        sb.append("      node {                                                ");
        sb.append("        target {                                            ");
        sb.append("          ... on Commit {                                   ");
        sb.append("            history(first: 1) {                             ");
        sb.append("              nodes {                                       ");
        sb.append("                oid                                         ");
        sb.append("              }                                             ");
        sb.append("            }                                               ");
        sb.append("          }                                                 ");
        sb.append("        }                                                   ");
        sb.append("      }                                                     ");
        sb.append("    }                                                       ");
        sb.append("    pageInfo {                                              ");
        sb.append("      endCursor                                             ");
        sb.append("    }                                                       ");
        sb.append("  }                                                         ");
        sb.append(" }                                                           ");
        sb.append("}");
        return sb.toString().replaceAll("  ", "");
    }

    private String replaceCrlf(String v) {
        return v.replaceAll("\\\n", "\\\\\\n").replaceAll("\\\r", "\\\\\\r");
    }

    private String escapeInputMessage(String v) {
        String cv = (v == null) ? "" : replaceCrlf(v);
        return escapeInput(cv, false);
    }

    private String escapeInput(String v) {
        return escapeInput(v, true);
    }

    private String escapeInput(String v, boolean needToValid) {
        if (needToValid) {
            Assert.hasLength(v, "The input must not null or has text!");
            ;
        }
        return v.replaceAll("\"", "\\\"");
    }

    /**
     * @param body
     * @return
     */
    private String formatRequestBody(String body) {
        body = body.replaceAll("\"", "\\\\\"");
        return String.format("{\"query\": \"%s\"}", body);
    }

    private String post(String url, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        ResponseEntity<String> rbody = client.exchange(url, HttpMethod.POST, new HttpEntity<String>(body, headers),
                String.class);
        if (rbody.getStatusCode().is2xxSuccessful()) {
            return rbody.getBody();
        }
        throw new GithubRemoteRepositoryOperationException(
                "Failed to request Http, reason:" + rbody.getStatusCode().getReasonPhrase());
    }

}
