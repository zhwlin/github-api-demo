package code.commit.gitlab;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import code.commit.CodeCommitOperation;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author linzhw
 * @version 1.0
 *
 */
@TestMethodOrder(OrderAnnotation.class)
class TestGithubApiCommit {

    String userToken = System.getenv("GITHUB_TOKEN");
    String owner = Optional.ofNullable(System.getenv("GITHUB_OWNER")).orElse("zhwlin");
    String repoName = Optional.ofNullable(System.getenv("GITHUB_REPO")).orElse("first-cmake-demo");
    String branch = Optional.ofNullable(System.getenv("GITHUB_BRANCH")).orElse("test");;
    
    
    @Test
    @Order(1)
    @DisplayName("Add one file:HELP.md")
    void addOneFile() throws IOException {
        String headLine =  "Add one file:HELP.md";
        SourceFilesChangeListBuilder changes = SourceFilesChangeListBuilder.create()
                                    .addUpdateFile(Paths.get("HELP.md"), "HELP.md")
                                    ;
        commit(headLine, changes);
    }
    
    @Test
    @Order(2)
    @DisplayName("Add twpo file:HELP.md,  src/main/resources/static/h2-1.4.200.jar")
    void addTwoFile() throws IOException {
        String headLine =  "Add twpo file:HELP.md, src/main/resources/static/h2-1.4.200.jar";
        SourceFilesChangeListBuilder changes = SourceFilesChangeListBuilder.create()
                                        .addUpdateFile(Paths.get("HELP.md"), "HELP.1.md")
                                        .addUpdateFile(Paths.get("src/main/resources/static/h2-1.4.200.jar"), "src/main/resources/static/h2-1.4.200.jar") //2m
//                                        .addUpdateFile(Paths.get("src/main/resources/static/h2-1.4.200.jar"), "src/main/resources/static/h2-1.4.200.jar.1") //2m
                                    ;
        commit(headLine, changes);
    }
    
    
    @Test
    @Order(3)
    @DisplayName("Update file:HELP.md")
    void updateFile() throws IOException {
        String headLine =  "Update file all HELP.md: read 'settings.gradle' file...";
        SourceFilesChangeListBuilder changes = SourceFilesChangeListBuilder.create()
                                        .addUpdateFile(Paths.get("settings.gradle"), "HELP.md")
                                    ;
        commit(headLine, changes);
    }
    
    
    @Test
    @Order(4)
    @DisplayName("Delete file:HELP.1.md, update file:HELP.md")
    void updateAndDeleteFile() throws IOException {
        String headLine =  "Delete file:HELP.1.md, update file:HELP.md";
        SourceFilesChangeListBuilder changes = SourceFilesChangeListBuilder.create()
                                                .addDeleteFile("HELP.1.md")
                                              .addUpdateFile(Paths.get("HELP.md"), "HELP.md")
                                    ;
        commit(headLine, changes);
    }

    
    @Test
    @Order(5)
    @DisplayName("Delete all file TestUnit ")
    void deleteAllCommitFiles() throws IOException {
        String headLine =  "Delete all file TestUnit ";
        SourceFilesChangeListBuilder changes = SourceFilesChangeListBuilder.create()
                                    .addDeleteFile("HELP.md")
                                    .addDeleteFile("src/main/resources/static/h2-1.4.200.jar")
//                                    .addDeleteFile("src/main/resources/static/h2-1.4.200.jar.1")
                                    ;
        commit(headLine, changes);
    }


    /**
     * @param headLine
     * @param changes
     */
    private void commit(String headLine, SourceFilesChangeListBuilder changes) {
        CodeCommitOperation action = new GithubCommitOperation();
        CommitInput ci = new CommitInput()
                             .setBranch(branch)
                             .setOwner(owner)
                             .setRepoName(repoName)
                             .setUserToken(userToken)
                             .setCommitMsgHeader(headLine)
                             .setCommitMsgBody(changes.getChangeList())
                             .setChangeList(changes);
        action.doAction(ci);
    }
}
