# Scripts for obtaining patent data

  * `uspto_download`: download US patent data

___


## `patent-data`
java project for implement tool to retrieve patent data
  
## `patent-data-libs`
java libraries build from patent-data project to retrieve patent data

1. `patent-search.jar`

   *Usage*
   
   ```
   java -jar patent-search.jar <yyyyMM>
   ```
  
   *Parameters*
  
   - `<yyyyMM>` specifies the year and month to search patent data

   *Description*

   - Call the OPS API to get all patents published within a month
   - Data will be save into `WorkingDir`/search/

2. `patent-extractids.jar`

   *Usage*
   
   ```
   java -jar patent-extractids.jar -I <input_dir> -O <output_dir>
   ```
  
   *Parameters*
  
   - `<input_dir>` specifies the directory path to the source file process for extraction
   - `<output_dir>` specifies the directory path to the output file process after extraction

   *Description*

   - Lookup xml file into `<input_dir>`
   - Get the document id from xml and save to `<output_dir>` 

3. `patent-getfamily.jar`

   *Usage*
   
   ```
   java -jar patent-getfamily.jar -I <input_path> -D <document_id>
   ```
  
   *Parameters*
  
   - `<input_path>` specifies the directory or file path to the source ids file to get patent family
   - or
   - `<document_id>` specifies the directory id to get patent family
   
   
   > Input file path or folder (txt only) or Document Id (CountryCode.DocNo.KindCode)

   *Description*
   
   - Get the family data for each ID
   - Extract and store the family data in a database in an easy to query format

4. `patent-getpair.jar`

   *Usage*
   
   ```
   java -jar patent-getpair.jar <source_country> <target_country> -O <output_path>
   ```
  
   *Parameters*
  
   - `<source_country>` specifies the source country to get pair data
   - `<target_country>` specifies the target country to get pair data
   - `<output_path>` specifies the path to the output file (txt only)
   
   *Description*

   - Query based on language pair

  > Note - you cannot get the language of the document from the family, only the location. You have to pull the document to get the actual language.
  
  
## patent-data.json

patent-data configuration file, put it into the patent-data installation path beside .jar file.

```
{
	"ConsumerKey": "",
	"ConsumerSecretKey": "",
	"Protocol": "https",
	"Host": "ops.epo.org",
	"AuthenURL": "/3.2/auth/accesstoken",
	"ServiceURL": "/3.2/rest-services/",
	"WorkingDir": "/work/",
	"DbHost": "localhost",
	"DbPort": "3306",
	"DbSchema": "patent",
	"DbUser": "",
	"DbPassword": ""
}
```

- `ConsumerKey`  specifies the consumer key
- `ConsumerSecretKey`  specifies the consumer secret
- `Protocol`  specifies the protocol used for api
- `Host`  specifies the host used for api
- `AuthenURL`  specifies the authen url used for api
- `ServiceURL`  specifies the service url used for api
- `WorkingDir`  specifies the working directory for the process
- `DbHost`  specifies the database host
- `DbPort`  specifies the database port
- `DbSchema`  specifies the database schema
- `DbUser`  specifies the database user
- `DbPassword`  specifies the database password


## patent-header.sql

database script to create patent_header table 

