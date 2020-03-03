# Scripts for obtaining patent data

  * `uspto_download`: download US patent data

___


## `patent-data`
java project for implement tool to retrieve patent data

## Using `patent.jar`
Patent-data can be used as a command line tool in 4 different methods.
   
```

java -jar patent.jar --help

------------------------
Select method: -search or -extractid or -getfamily or -getpair (Required)
------------------------

```

   
**1. Search Patent**

   *Usage*

   ```

   java -jar patent.jar -search [--help] <yyyyMM> -C <config_file>

   ------------------------
   Parameters
   ------------------------
   1. <Date> (Required): Date to search patents (YYYYMMDD)
   2. -C <ConfigPath> (Optional): JSON configuration file.
   ------------------------
   Example
   ------------------------
   java -jar patent.jar -search "20191231" -C "config/path/patent.json"
   ------------------------

   ```

   *Description*

   - Call the OPS API to get all patents published within a date
   - Data will be save into directory `WorkingDir`/search/




**2. Extract Document Id**

   *Usage*
   
   ```
   
   java -jar patent.jar -extractid [--help] -I <input_dir> -O <output_dir> -C <config_file>
   
   ------------------------
   Parameters
   ------------------------
   1. -I <InputDir> (Required): Input folder
   2. -O <OutputDir> (Required): Output folder
   3. -C <ConfigPath> (Optional): JSON configuration file
   ------------------------
   Example
   ------------------------
   java -jar patent.jar -extractid -I "input/directory" -O "output/directory" -C "config/path/patent.json"
   ------------------------

   ```
  
   *Description*

   - Lookup xml file into `<input_dir>`
   - Get the document id from xml and save to directory `<output_dir>`




**3. Get Family Data**

   *Usage*
   
   ```
   java -jar patent.jar -getfamily [--help] -I <input_path> -D <document_id> -C <config_file>
   
   ------------------------
   Parameters
   ------------------------
   1. -I <InputPath> or -D <DocId> (Required): Input file path or folder (TXT only) or Document Id (CountryCode.DocNo.KindCode)
   2. -C <ConfigPath> (Optional): Config path
   ------------------------
   Example
   ------------------------
   java -jar patent.jar -getfamily -I "input/path/ids_201912.txt" -C "config/path/patent.json"
   java -jar patent.jar -getfamily -D "JP.H07196059.A" -C "config/path/patent.json"
   ------------------------
   
   ```
  
   *Description*
   
   - Get the family data for each ID
   - Extract and store the family data in a database in an easy to query format




**4. Get Patent Pair Data**

   *Usage*
   
   ```
   
   java -jar patent.jar -getpair [--help] <source_country> <target_country> -O <output_path> -C <config_file>
   
   ------------------------
   Parameters
   ------------------------
   1. <SourceCountry> (Required): Source Country
   2. <TargetCountry> (Required): Target Country
   3. -O <OutputPath> (Required): Output file path (TXT only)
   4. -C <ConfigPath> (Optional): Config path
   ------------------------
   Example
   ------------------------
   java -jar patent.jar -getpair "TW" "US" -O "input/path/pair_result.txt" -C "config/path/conf.json"
   ------------------------
   
   ```
  
   *Description*


   - Query based on language pair
  

> If JSON configuration is not specified, then the file `patent.json` in the same folder as the `patent.jar` file will be used.

> Note - you cannot get the language of the document from the family, only the location. You have to pull the document to get the actual language.
 
 
 
  
## patent.json

patent configuration 
ile, put it into the patent-data installation path b
side .jar file.

```
{
	"ConsumerKey": "",
	"ConsumerSecretKey": "",
	"Protocol": "https",
	"Host": "ops.epo.org",
	"AuthenURL": "/3.2/auth/accesstoken",
	"ServiceURL": "/3.2/rest-services/",
	"WorkingDir": "/work",
	"DbDriver": "org.mariadb.jdbc.Driver",
	"Jdbc": "jdbc:mariadb",
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
- `DbDriver`  specifies the database driver
- `Jdbc`  specifies the database vendor
- `DbHost`  specifies the database host
- `DbPort`  specifies the database port
- `DbSchema`  specifies the database schema
- `DbUser`  specifies the database user
- `DbPassword`  specifies the database password


## patent_docno.sql

database script to create patent_docno table 

