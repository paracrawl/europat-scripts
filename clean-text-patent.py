#!/usr/bin/env python3
import sys
import re
import html

# Remove inline unwanted
pRemoveInline = re.compile("(<\\?delete-start.*?(<\\?delete-end).*?>)|<script .+?>.+?<\\/script>|<!--.+?-->")

# Replace unprocessable content with datatype markers so that alignment can still work
pReplaceUnprocessable1 = re.compile("(<maths.+?>.+?<\\/maths>|<MATH-US.+?>.+?<\\/MATH-US>)")
pReplaceUnprocessable2 = re.compile("(<[?]in-line-formulae .+?[?]>).+?(<[?]in-line-formulae .+?[?]>)")

#Add spacing
sInline = "a|abbr|acronym|b|bdo|big|br|button|cite|dfn|em|i|input|kbd|label|map|object|output|q|samp|select|small|span|strong|time|tt|var"
sInlineDtd = "|b|i|u .+?|u|o .+?|o|dl .+?|dl|ol .+?|ol|patcit .+?|patcit|nplcit .+?|nplcit|rel-passage|text|ul .+?|ul"
sInlineClaim = "|claim .+?|claim|claim-ref .+?|claim-ref|smallcaps|pre .+?|pre|amended-claims .+?|amended-claims|amended-claims|amended-claims-statement .+?|amended-claims-statement|claims-statement|chemistry .+?|chemistry|figref .+?|figref|crossref .+?|crossref"
sInlineTable = "|tables .+?|tables|table .+?|table|tgroup .+?|tgroup|thead .+?|thead|tbody .+?|tbody|row .+?|row"
sInlineAbstract = "|abstract .+?|abstract|abst-problem .+?|abst-problem|abst-solution .+?|abst-problem"
sInLineEmpty = "<(colspec .+?)\\/>|<(chem .+?)\\/>|<(img .+?)\\/>|<[?].+?[?]>|<entry/>"
sSup = "sup2|sub2|sup|sub"
uspto = "|PDAT|HIL|SB|CHEMCDX .+?|CHEMMOL .+?|CHEMMOL|CHEM-US .+?|CHEM-US|EMI .+?|EMI|CLREF .+?|CLREF|" \
		+ "ITALIC|CL|H .+?|F|BOLD|CLREF|TABLE-.+?|CLMSTEP.+?|H|COLSPEC.+?|TBODY.+?|TGROUP.+?|FGREF.+?|SMALLCAPS.+?|TABLE .+?|CUSTOM-CHARACTER .+?" \
		+ "|S-1-.+?|S-2-.+?|SP"
pInlineSpacing = re.compile("<\\/(" + sInline + sInlineClaim + sInlineTable + sInlineAbstract + sInlineDtd + uspto \
		+ ")>|" + "<(" + sInline + sInlineClaim + sInlineTable + sInlineAbstract + sInlineDtd + uspto \
		+ ")>|<(\\/{0,1})(" + sSup + ")>" + "|<(" + sSup + ")>|<br\\/>|" + sInLineEmpty)
pRemainingTags1 = re.compile("(<\\/p><p>){2,}|(<\\/p><p>)[ ]*(<\\/p><p>)")
pRemainingTags2 = re.compile("([ ]{2,})")
pRemainingTags3 = re.compile("(<\\/p><p>)[ ]*\\t")
pRemainingTags4 = re.compile("(<\\/p><p>[ ]*)\n")
pRemainingTags5 = re.compile("\\t[ ]*<\\/p><p>")
pRemainingTags6 = re.compile("<p>[ ]<\\/p>")
pRemainingTags7 = re.compile("<(?!\\/?p(?=>|\\s[a-z0-9\\W ]*>))\\/?[a-z0-9\\W ]*?>")
pRemainingTags8 = re.compile("((?P<rpl>:|;)(<br>|<br/>))")
pRemainingTags9 = re.compile("<p[ ]+id.+?>")
pRemainingTags10 = re.compile("(<\\/p>){2,}")
pRemainingTags0 = re.compile("(<(?P<rpl>[0-9]+(\\-*|[ ]*))>)|(<(" + sSup + ")>(?P<rpl1>.+?)<\\/(" + sSup + ")>)")
sBullet3 = "(([a-z]{1}[0-9]*\\.*[ ]*)?((([0-9]{1,10}\\.[ ]*){0,})([0-9]{1,10})(\\.|[)\\]]{0,3})))"
sBullet1 = "|((([0-9]{1,})|([a-z]{1})|(XXXVIIIXC|LXXXVIII|CXCVIII|LXXVIII|LXXXIII|LXXXVII|XXXIII|XCVIII|XXXVII|LXVIII|LXIXXC|LXXIII|DCCVII|DCCCXC|LXXVII|XLVIII|LXXXII|LXXXIV|LXXXVI|LXXXIX|XXVIII|XXXII|LXIII|XXXIV|XCVII|XXXVI|LXVII|XXXIX|LXXII|XLIII|LXXIV|LXXVI|XLVII|MDCCC|LXXIX|XVIII|LXXXI|XXIII|LXXXV|XXVII|LVIII|XCIII|VIII|XCIV|LXIV|XCVI|XXXV|LXVI|XCIX|LXXI|XLII|DXXX|XIII|XLIV|LXXV|XLVI|XVII|XLIX|LXXX|XXII|LIII|XXIV|XXVI|LVII|XXIX|XCII|XXXI|LXII|XCV|III|LXV|VII|LXX|XLI|XII|XIV|XLV|XVI|XIX|XXI|LII|LIV|XXV|LVI|LIX|XCI|XXX|LXI|II|IV|VI|IX|XL|DI|XI|DL|XV|MD|CM|XX|LI|LV|XC|LX|I|V|X|L))[.\\])]{0,3}\\.)"
sBullet2 = "|(-{1,}|[+]{1,})|(([0-9]{1,10}\\.[ ]*){0,10}[0-9]{1,10}\\([0-9a-zA-Z]{1,2}([\\)]?[\\,]?[\\(]?[0-9a-zA-Z]{1,2}){0,5}[)]{1,3})"
sBullet4 = "|((([(\\[]{1,3})(([0-9]{1,10})|([a-z]{1,3}))([)\\]]{1,3})){1,})"
sBullet5 = "|((((XXXVIIIXC|LXXXVIII|CXCVIII|LXXVIII|LXXXIII|LXXXVII|XXXIII|XCVIII|XXXVII|LXVIII|LXIXXC|LXXIII|DCCVII|DCCCXC|LXXVII|XLVIII|LXXXII|LXXXIV|LXXXVI|LXXXIX|XXVIII|XXXII|LXIII|XXXIV|XCVII|XXXVI|LXVII|XXXIX|LXXII|XLIII|LXXIV|LXXVI|XLVII|MDCCC|LXXIX|XVIII|LXXXI|XXIII|LXXXV|XXVII|LVIII|XCIII|VIII|XCIV|LXIV|XCVI|XXXV|LXVI|XCIX|LXXI|XLII|DXXX|XIII|XLIV|LXXV|XLVI|XVII|XLIX|LXXX|XXII|LIII|XXIV|XXVI|LVII|XXIX|XCII|XXXI|LXII|XCV|III|LXV|VII|LXX|XLI|XII|XIV|XLV|XVI|XIX|XXI|LII|LIV|XXV|LVI|LIX|XCI|XXX|LXI|II|IV|VI|IX|XL|DI|XI|DL|XV|MD|CM|XX|LI|LV|XC|LX|I|V|X|L)|([a-z]{1}))\\.[ ]*)((([0-9]{1,10}\\.[ ]*){0,})([0-9]{1,10})))"
sBullet6 = "|(([0-9]{1,10}\\.[ ]*){1,}([0-9]{1,10}))"
sBullet7 = "|([a-z]{1}[0-9]{0,10}(\\.|([)\\]])))"
sBullet8 = "|((\\(|\\[|\\{)([a-z]{1}|[0-9]{1,10})(\\.[ ]*([a-z]{1}|[0-9]))*(\\)|\\]|\\}))\\.*"
sBullet = sBullet3 + sBullet1 + sBullet2 + sBullet4 + sBullet5 + sBullet6 + sBullet7 + sBullet8
pBullet = re.compile("(<p>[ ]*)("+ sBullet + ")([ ]:+[ ]*|[ ])", re.IGNORECASE)
pBullet1 = re.compile("(\\t|<br>[ ]*|<br\\/>[ ]*)("+ sBullet + ")([ ]*|<br>|<br\\/>)", re.IGNORECASE)
pBracketOpen = re.compile("(\\(|\\{|\\[)[ ]")
pBracketClose = re.compile("[ ](\\)|\\}|\\])")
pSpChar = re.compile("((\\u2003){3,})")
pSpChar2 = re.compile("(\\u2003)+")

def process(text):
	text = pBullet1.sub(r"\1</p><p>", text);
	text = pRemainingTags0.sub(r"\g<rpl>\g<rpl1>", text);
	text = pRemainingTags8.sub(r"\g<rpl></p><p>", text);
	
	# Remove inline unwanted
	text = pRemoveInline.sub(" ", text);

	# Remove Inline
	text = pInlineSpacing.sub(" ", text);

	# Replace unprocessable content with datatype markers so that alignment can
	text = pReplaceUnprocessable1.sub(" EPMATHMARKEREP ", text);
	text = pReplaceUnprocessable2.sub(" EPFORMULAMARKEREP ", text);
	##text = pReplaceUnprocessable3.sub(" EPDOCIDMAKER ", text);
	##				text = pRemainingTags0.sub("@STTAG@", text);
	text = pRemainingTags2.sub(" ", text);
	##				text = pRemainingTags0.sub("</p><p>", text);
	text = pRemainingTags9.sub("</p><p>", text);
	text = pRemainingTags7.sub("</p><p>", text);

	text = pRemainingTags6.sub("", text);
	text = pRemainingTags1.sub("</p><p>", text);

	text = pRemainingTags3.sub("</p>\t", text);
	text = pRemainingTags5.sub("\t<p>", text);
	text = pRemainingTags4.sub("</p>\n", text);
	text = pBracketOpen.sub(r"\1", text);
	text = pBracketClose.sub(r"\1", text);

	text = pBullet.sub("<p>", text);
	# to handle the bullet contain space: 10. 10. 1. 2. 12.
	text = pBullet.sub("<p>", text);
	text = pSpChar2.sub(" ", text);
	text = pRemainingTags1.sub("</p><p>", text);
	text = pRemainingTags10.sub("</p>", text);

	text = html.unescape(text);

	return text

def main():
	for line in sys.stdin:
		print(process(line.rstrip()))

if __name__ == '__main__':
	main()