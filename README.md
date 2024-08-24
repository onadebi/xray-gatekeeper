#  <span style='color:cornflowerblue'>Xray-gatekeeper</span>

Xray GateKeeper is a SpringBoot application designed to act as a gatekeeper for applications interacting with XRay cloud to public results to Jira. Being API based, it can work with Playwright and Java test frameworks.
## Variables:

The below environment variables are needed to be set for the application to run smoothly.
```json
RATE_DURATION=120000
RATE_LIMIT=3

XRAY_DATABASE=###[Name of application database]
XRAYDB_PASSWORD=###[Application database user password]
XRAYDB_USER=###[Application database user]

RABBITMQ_HOST=###
RABBITMQ_PORT=###
RABBITMQ_USER=###
RABBITMQ_PASSWORD=###

XRayOpExchange=###[For defning the MessageQueue Exchange name]
XRayOpQueue=###[For defning the MessageQueue Queue name]
XRayRoutingKey=###[For defning the MessageQueue RoutingKey name]
```
The API by default is configured to use a MariaDB driver with MariaDBDialect. Hence, the `XRAY_DATABASE` should be a MariaDB database.
## API Endpoints:

The application would provide several endpoints for interacting with Xray cloud. The base endpoint is of the format ```http://localhost:8080/api/v2/xray``` . The inputs format for the endpoints may vary depending on endpoint being executed. Generally the response would have a format of:
```json
{
    "result": "",
    "error": null,
    "success": true,
    "statCode": 200
}
```
**Results** would contain any response from the server and may be a simple string or object type. **Success** is always a boolean indicating of the intended operation was successful or not. **Statcode** would convey the server status code response and **Error** may be null if no errors or convey any error message if present.
<br/>Some of these endpoints include:

### 1. **XRay Authentication** (`/authenticate`): 
>This endpoint is used to authenticate against the XRay cloud. The endpoint expects input parameters in JSON format as defined below:
```json
{
    "client_id": "******",
    "client_secret": "******"
}
```

### 2. **Upload JUnit Xml Report** (`/junit/multipart`):
>This endpoint passes to the server as header, a Bearer token gotten from successful authentication from `/authenticate` endpoint. Two files are passed `xray-report.xml` and `project-info.json` as parameters `results` and `info` respectively.
<p>The <code>xray-report.xml</code> has sample format:</p>

```xml
<testsuites id="" name="" tests="2" failures="0" skipped="0" errors="0" time="16.337399000003934">
	<testsuite name="example.spec.ts" timestamp="2024-08-12T21:48:13.173Z" hostname="chromium" tests="2" failures="0" skipped="0" time="26.429" errors="0">
		<testcase name="Page has valid title" classname="example.spec.ts" time="12.859">
		</testcase>
		<testcase name="Get started link" classname="example.spec.ts" time="13.57">
		</testcase>
	</testsuite>
</testsuites>
```
<p>The <code>project-info.json</code> has sample format:</p>

```json
{
    "fields": {
        "project": {
            "id": "10006"
        },
        "summary": "JDPA new Test execution",
        "issuetype": {
            "id": "10025"
        },
        "labels" : ["Epic"]
    }
}
```
<i>issuetype</i> is Jira IssueType '<i>Test Execution</i>' id.


### 3. **Upload Cucumber Json Report** (`/cucumber/multipart`):
>This endpoint passes to the server as header, a Bearer token gotten from successful authentication from `/authenticate` endpoint. Two files are passed `results.json` and `issueFields.json` as parameters `results` and `info` respectively.
<p>The <code>results.json</code> has sample format:</p>

```json
[
  {
    "description": "",
    "elements": [
      {
        "description": "",
        "id": "validate-that-entries-on-home-are-consistent-in-functionality-and-expected-values;validate-count-of-available-pizzas-is-same-as-that-of-the-listed",
        "keyword": "Scenario",
        "line": 4,
        "name": "Validate count of available pizzas is same as that of the listed",
        "steps": [
          {
            "keyword": "Before",
            "hidden": true,
            "result": {
              "status": "passed",
              "duration": 1074620900
            }
          },
          {
            "arguments": [],
            "keyword": "Given ",
            "line": 5,
            "name": "Home page should have a title",
            "match": {
              "location": "src\\test\\steps\\homepagesteps.spec.ts:21"
            },
            "result": {
              "status": "passed",
              "duration": 12860438000
            }
          }
		  ],
        "tags": [
          {
            "name": "@CORP-42",
            "line": 3
          }
        ],
        "type": "scenario"
      }
    ],
    "id": "validate-that-entries-on-home-are-consistent-in-functionality-and-expected-values",
    "line": 1,
    "keyword": "Feature",
    "name": "Validate that entries on home are consistent in functionality and expected values",
    "tags": [],
    "uri": "src\\test\\features\\homepage.feature"
  },
  {
    "description": "",
    "elements": [],
    "id": "creation-of-custom-pizza-with-selected-toppings",
    "line": 1,
    "keyword": "Feature",
    "name": "Creation of custom Pizza with selected toppings",
    "tags": [],
    "uri": "src\\test\\features\\pizzacreations.feature"
  }
]
```
<p>The <code>issueFields.json</code> has sample format:</p>

```json
{
  "fields": {
    "project": {
      "id": "10006"
    },
    "summary": "JDPA new Test execution",
    "issuetype": {
      "id": "10025"
    },
    "labels" : ["Epic"]
  },
  "xrayFields": {
    "testPlanKey": "CORP-41"
  }
}
```
<i>testPlanKey</i> is jira issue of type '<i>Test Plan</i>' key.

### 4. **Features upload** (`/feature?projectKey={corp}`):
>This endpoint receives in the request, a Bearer token gotten from successful authentication from `/authenticate` endpoint. Also, the projectKey is passed as a URL Request parameter.
<br/>Additionally, a feature file (`*.feature`) or zipped file (`*.zip`) of feature files is expected to be passed as a form-data body to the parameter name `file`.


### 5. **Cancel Request** (`/cancel/{requestId}`):
>This endpoint is responsible for cancelling any requests pending in the message queue and that has not been executed, but rather has a PENDING status in the database table.


### 6. **Health Check** (`/health`, `/status`):
>This endpoint is used to check the health/status of the Springboot application status; if its running successfully or not
## Add your files

- [ ] [Create](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#create-a-file) or [upload](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#upload-a-file) files
- [ ] [Add files using the command line](https://docs.gitlab.com/ee/gitlab-basics/add-file.html#add-a-file-using-the-command-line) or push an existing Git repository with the following command:

```
cd existing_repo
git remote add origin https://git.autodatacorp.org/corp/automation/libraries/xray-gatekeeper.git
git branch -M main
git push -uf origin main
```