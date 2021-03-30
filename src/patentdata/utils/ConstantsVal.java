package patentdata.utils;

import java.util.Arrays;
import java.util.List;

public class ConstantsVal {

	public static final String YES = "Y";
	public static final String NO = "N";
	public static final String StartTitle ="EPTAG:TITLE:";
	public static final String EndTitle ="\nEPTAG:TITLEEND:";
	public static final String StartAbstract ="EPTAG:ABSTRACT:";
	public static final String EndAbstract ="\nEPTAG:ABSTRACTEND:";
	public static final String StartDscp ="EPTAG:DESCRIPTION:";
	public static final String EndDscp ="\nEPTAG:DESCRIPTIONEND:";
	public static final String StartClaims ="EPTAG:CLAIMS:";
	public static final String EndClaims ="\nEPTAG:CLAIMSEND:";
	public static final String StartMetadata ="EPTAG:MTDT:";
	public static final String EndMetadata ="\nEPTAG:MTDTEND:";
	
	public static final String SP_TITLE 		= "p_pair_pt_@slang@_@tlang@_title";
	public static final String SP_ABSTRACT 		= "p_pair_pt_@slang@_@tlang@_abstract";
	public static final String SP_dscp			= "p_pair_pt_@slang@_@tlang@_dscp";
	public static final String SP_CLAIM 		= "p_pair_pt_@slang@_@tlang@_claim";
	public static final String SP_METADATA		= "p_pair_pt_@slang@_@tlang@_metadata";
	
	public static final String SP_PAIR_ID		= "p_pair_pt_doc_id_@slang@_@tlang@";
	
	public static final String TABLE_TITLE 		= "t_@source@_@lang@_pt_title";
	public static final String TABLE_ABSTRACT 	= "t_@source@_@lang@_pt_abstract";
	public static final String TABLE_dscp		= "t_@source@_@lang@_pt_dscp";
	public static final String TABLE_CLAIM 		= "t_@source@_@lang@_pt_claim";
	public static final String TABLE_METADATA	= "t_@source@_@lang@_pt_metadata";
	
	public static final String TABLE_TITLE_TMP 		= "t_@source@_@lang@_pt_title_tmp";
	public static final String TABLE_ABSTRACT_TMP 	= "t_@source@_@lang@_pt_abstract_tmp";
	public static final String TABLE_dscp_TMP		= "t_@source@_@lang@_pt_dscp_tmp";
	public static final String TABLE_CLAIM_TMP 		= "t_@source@_@lang@_pt_claim_tmp";
	public static final String TABLE_METADATA_TMP	= "t_@source@_@lang@_pt_metadata_tmp";
	
	public static final String TABLE_US_EN_TITLE 		= "t_us_en_pt_title";
	public static final String TABLE_US_EN_ABSTRACT 	= "t_us_en_pt_abstract";
	public static final String TABLE_US_EN_dscp			= "t_us_en_pt_dscp";
	public static final String TABLE_US_EN_CLAIM 		= "t_us_en_pt_claim";
	public static final String TABLE_US_EN_METADATA		= "t_us_en_pt_metadata";

	public static final String TABLE_EP_DE_TITLE 		= "t_ep_de_pt_title";
	public static final String TABLE_EP_DE_ABSTRACT 	= "t_ep_de_pt_abstract";
	public static final String TABLE_EP_DE_dscp			= "t_ep_de_pt_dscp";
	public static final String TABLE_EP_DE_CLAIM 		= "t_ep_de_pt_claim";
	public static final String TABLE_EP_DE_METADATA		= "t_ep_de_pt_metadata";
	
	public static final String TABLE_EP_FR_TITLE 		= "t_ep_fr_pt_title";
	public static final String TABLE_EP_FR_ABSTRACT 	= "t_ep_fr_pt_abstract";
	public static final String TABLE_EP_FR_dscp			= "t_ep_fr_pt_dscp";
	public static final String TABLE_EP_FR_CLAIM 		= "t_ep_fr_pt_claim";
	public static final String TABLE_EP_FR_METADATA		= "t_ep_fr_pt_metadata";
	
	public static final String TABLE_EP_EN_TITLE 		= "t_ep_en_pt_title";
	public static final String TABLE_EP_EN_ABSTRACT 	= "t_ep_en_pt_abstract";
	public static final String TABLE_EP_EN_dscp			= "t_ep_en_pt_dscp";
	public static final String TABLE_EP_EN_CLAIM 		= "t_ep_en_pt_claim";
	public static final String TABLE_EP_EN_METADATA		= "t_ep_en_pt_metadata";
	
	public static final String SOURCE_USPTO				= "us";
	public static final String SOURCE_EUROPAT			= "ep";
	public static final String SOURCE_OTHERS			= "ohters";
	
	public static final String LANG_EN					= "en";
	public static final String LANG_DE					= "de";
	public static final String LANG_FR					= "fr";
	
	public static final String TITLE 		= "title";
	public static final String ABSTRACT 	= "abstract";
	public static final String DSCP			= "dscp";
	public static final String CLAIM 		= "claim";
	public static final String METADATA		= "metadata";
	public static final String PATENT_ID	= "patentid";
	
	public static final List<String> LIST_SOURCE 	= Arrays.asList(SOURCE_USPTO, SOURCE_EUROPAT); 
	public static final List<String> LIST_LANG 		= Arrays.asList(LANG_EN, LANG_DE, LANG_FR); 
	public static final List<String> LIST_LANG_SOURCE 		= Arrays.asList(LANG_DE, LANG_FR); 
	public static final List<String> LIST_LANG_TARGET 		= Arrays.asList(LANG_EN); 
	public static final List<String> LIST_CATEGORY	= Arrays.asList(TITLE, ABSTRACT, DSCP, CLAIM, METADATA, PATENT_ID); 
	public static final List<String> LIST_CONTENTTYPE_EPO	= Arrays.asList("description", "claims", "abstract,biblio", "abstract", "biblio"); 

	
	
	
	

}
