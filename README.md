# HTML Template Editor & Printer

#### Goal:

Given user input data, edit a prefab HTML template and print it. The main purpose of this app is to make the process of creating delivery and reception notes for products easier.

#### How the app should work:

##### - Configuration:
* The user loads an HTML template file. Required fields should be enclosed within the HTML file with double brackets: `{{ }}`
* The app scans for required fields inside the input file. For each scanned field, it prompts the user to configure the desired text to be shown when using the app. For example, if the field `{{NAME}}` is scanned, the user might want the app to show the text `Name` or `Full name` when requesting the input data.
* After every field is configured, the app starts.

##### - Usage:
* The user selects the desired profile.
* The app prompts the user to input values for each required field inside the HTML template.
* The user inputs the requested data and clicks the "Create & Print" button.
* The app loads the field values, creates the HTML file and prints it.

#### Installation steps

Requirements: Java JDK 21, Maven

1) Clone this repository: `git clone https://github.com/Bunshock/html-template-editor-to-print.git`. 
2) In a command line run `mvn clean install` inside the root folder containing the `pom.xml` file to download dependencies.
3) Run the application with `mvn javafx:run`.


#### Package creation steps

Requirements: JAVA_HOME properly configured in system environment variables

1) Create package. Under the root folder containing the `pom.xml` file run: `mvn clean package`
2) Create release with executable (.exe). Run: `jpackage --name "<release_name>" --input target --main-jar "<.jar_filename_under_target_folder>" --main-class com.bunshock.Launcher --type app-image --dest "release" --win-console`
3) Locate the release folder and execute. To distribute, compress the release folder (and app data if needed) and share.
