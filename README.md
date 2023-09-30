# A source code commit tool for Github


## Introduce
A client tool for committing code to Github repository with your user token.


## Usage

It works on JDK8 or above.

## Run Junit Test

－ Add system environment variables

|Name| Value|
| :--- | ---|
| GITHUB_OWNER| Your user name|
|GITHUB_REPO| The name of target repository|
|GITHUB_BRANCH| The name of target branch  |
|GITHUB_TOKEN| The access token|
 

- Run command:

  - Linux
    ```shell
    export GITHUB_TOKEN="tyyy"
    export GITHUB_REPO="hello"
    export GITHUB_BRANCH="ooop" 
    export GITHUB_OWNER="xx"

    gradlew test
    ```


