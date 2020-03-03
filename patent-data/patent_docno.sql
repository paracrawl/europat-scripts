CREATE TABLE `patent_docno` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doc_no` varchar(45) NOT NULL,
  `country_code` varchar(2) NOT NULL,
  `kind_code` varchar(2) DEFAULT NULL,
  `family_id` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
)