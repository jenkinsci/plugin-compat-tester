<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="html" omit-xml-declaration="no" indent="yes"/>

    <xsl:variable name="testedJenkinsCoordinates" select="/report/testedCoreCoordinates/coord[groupId='org.jenkins-ci.plugins']" />
    <xsl:variable name="otherCoreCoordinates" select="/report/testedCoreCoordinates/coord[not(groupId='org.jenkins-ci.plugins')]" />

    <xsl:template match="/">
<html>
    <head>
        <title>Plugin compat tester report</title>
        <style type="text/css">
th.pluginname{
    width: 200px;
}
th.version {
    width: 50px;
}
        </style>
    </head>
    <body>
        <table cellspacing="0" cellpadding="0" border="1">
            <tr>
               <th rowspan="2" colspan="2" class="pluginname">Plugin name</th>
               <xsl:element name="th">
                   <xsl:attribute name="colspan"><xsl:value-of select="count($testedJenkinsCoordinates)" /></xsl:attribute>
                   Jenkins core
               </xsl:element>
               <xsl:if test="count($otherCoreCoordinates) &gt; 0">
                   <xsl:element name="th">
                       <xsl:attribute name="colspan"><xsl:value-of select="count($otherCoreCoordinates)" /></xsl:attribute>
                       Other core
                   </xsl:element>
               </xsl:if>
            </tr>
            <tr>
                <xsl:for-each select="$testedJenkinsCoordinates" >
                    <th class="version">
                        <xsl:value-of select="version" />
                    </th>
                </xsl:for-each>
                <xsl:for-each select="$otherCoreCoordinates" >
                    <th class="version">
                        <xsl:value-of select="version" />
                    </th>
                </xsl:for-each>
            </tr>
            <xsl:apply-templates select="/report/pluginCompatTests/entry">
                <xsl:sort select="pluginInfos/pluginName" />
                <xsl:sort select="pluginInfos/pluginVersion" />
            </xsl:apply-templates>
        </table>
    </body>
</html>
    </xsl:template>

    <xsl:template match="/report/pluginCompatTests/entry">
        <xsl:variable name="currentEntry" select="." />
        <tr>
           <!-- Merging rows having same plugin name -->
           <xsl:if test="not(preceding-sibling::entry/pluginInfos/pluginName = $currentEntry/pluginInfos/pluginName)">
               <xsl:element name="td">
                   <xsl:attribute name="rowspan"><xsl:value-of select="count(//entry[pluginInfos/pluginName=$currentEntry/pluginInfos/pluginName])" /></xsl:attribute>
                   <xsl:value-of select="pluginInfos/pluginName/text()"/>
               </xsl:element>
           </xsl:if>
           <!-- Displaying plugin version -->
           <td><xsl:value-of select="pluginInfos/pluginVersion/text()"/></td>
           <!-- Displaying plugin compilation / test result for every tested core coordinates -->
           <xsl:for-each select="$testedJenkinsCoordinates">
                <xsl:call-template name="display-cell">
                    <xsl:with-param name="compatResult" select="$currentEntry/list/compatResult[./coreCoordinates/version = current()/version and ./coreCoordinates/groupId = current()/groupId and ./coreCoordinates/artifactId = current()/artifactId]" />
                </xsl:call-template>
           </xsl:for-each>
           <xsl:for-each select="$otherCoreCoordinates">
                <xsl:call-template name="display-cell">
                    <xsl:with-param name="compatResult" select="$currentEntry/list/compatResult[./coreCoordinates/version = current()/version and ./coreCoordinates/groupId = current()/groupId and ./coreCoordinates/artifactId = current()/artifactId]" />
                </xsl:call-template>
           </xsl:for-each>
        </tr>
    </xsl:template>

    <xsl:template name="display-cell">
        <xsl:param name="compatResult" />
        <td>
            <xsl:choose>
                <xsl:when test="count($compatResult)=0">-----</xsl:when>
                <xsl:otherwise>
		<xsl:choose>
			<xsl:when test="$compatResult/status = 'INTERNAL_ERROR'"><xsl:call-template name="display-internal-error"><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'COMPILATION_ERROR'"><xsl:call-template name="display-compilation-error"><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'TEST_FAILURES'"><xsl:call-template name="display-tests-failures"><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'SUCCESS'"><xsl:call-template name="display-success"><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
		</xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </td>
    </xsl:template>

    <xsl:template name="display-internal-error">
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="compilation-title">Compilation : Internal error !</xsl:with-param>
		<xsl:with-param name="compilation-img">red.png</xsl:with-param>
		<xsl:with-param name="compilation-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
        <xsl:with-param name="tests-title">Tests : Internal error !</xsl:with-param>
        <xsl:with-param name="tests-img">red.png</xsl:with-param>
        <xsl:with-param name="tests-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
        <xsl:with-param name="warnings" select="$compatResult/warningMessages//string" />
	</xsl:call-template>
    </xsl:template>
    
    <xsl:template name="display-compilation-error">
	<xsl:param name="compatResult" />
    <xsl:call-template name="display-result">
		<xsl:with-param name="compilation-title">Compilation : failure !</xsl:with-param>
		<xsl:with-param name="compilation-img">yellow.png</xsl:with-param>
		<xsl:with-param name="compilation-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
        <xsl:with-param name="tests-title">Tests : No tests executed !</xsl:with-param>
        <xsl:with-param name="tests-img">red.png</xsl:with-param>
        <xsl:with-param name="tests-error">Compilation failed => No tests executed !</xsl:with-param>
        <xsl:with-param name="warnings" select="$compatResult/warningMessages//string" />
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-tests-failures">
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="compilation-title">Compilation : Success !</xsl:with-param>
		<xsl:with-param name="compilation-img">blue.png</xsl:with-param>
		<xsl:with-param name="compilation-error">_</xsl:with-param>
        <xsl:with-param name="tests-title">Tests : Some tests are in failure !</xsl:with-param>
        <xsl:with-param name="tests-img">yellow.png</xsl:with-param>
        <xsl:with-param name="tests-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
        <xsl:with-param name="warnings" select="$compatResult/warningMessages//string" />
	</xsl:call-template>
    </xsl:template>
    
    <xsl:template name="display-success">
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="compilation-title">Compilation : Success !</xsl:with-param>
		<xsl:with-param name="compilation-img">blue.png</xsl:with-param>
		<xsl:with-param name="compilation-error">_</xsl:with-param>
        <xsl:with-param name="tests-title">Tests : Success !</xsl:with-param>
        <xsl:with-param name="tests-img">blue.png</xsl:with-param>
        <xsl:with-param name="tests-error">_</xsl:with-param>
        <xsl:with-param name="warnings" select="$compatResult/warningMessages//string" />
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-result">
        <xsl:param name="compilation-title"/>
        <xsl:param name="compilation-img" />
        <xsl:param name="compilation-error" />
        <xsl:param name="tests-title" />
        <xsl:param name="tests-img" />
        <xsl:param name="tests-error" />
        <xsl:param name="warnings" />
        <xsl:call-template name="display-img">
            <xsl:with-param name="title" select="$compilation-title" />
            <xsl:with-param name="img" select="$compilation-img" />
            <xsl:with-param name="error" select="$compilation-error" />
        </xsl:call-template>
        <xsl:call-template name="display-img">
            <xsl:with-param name="title" select="$tests-title" />
            <xsl:with-param name="img" select="$tests-img" />
            <xsl:with-param name="error" select="$tests-error" />
        </xsl:call-template>
        <xsl:if test="count($warnings) &gt; 0">
            <xsl:call-template name="display-img">
                <xsl:with-param name="title">Warnings : <xsl:value-of select="$warnings" /></xsl:with-param>
                <xsl:with-param name="img">document.gif</xsl:with-param>
                <xsl:with-param name="error">_</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="display-img">
        <xsl:param name="title" />
        <xsl:param name="img" />
        <xsl:param name="error" />

	<xsl:element name="img">
	    <xsl:attribute name="alt"><xsl:value-of select="$title" /></xsl:attribute>
	    <xsl:attribute name="title"><xsl:value-of select="$title" /><xsl:if test="not($error='_')"><xsl:value-of select="$error" /></xsl:if></xsl:attribute>
	    <xsl:attribute name="src">https://github.com/jenkinsci/jenkins/raw/master/war/src/main/webapp/images/24x24/<xsl:value-of select="$img" /></xsl:attribute>
	    <xsl:attribute name="width">24</xsl:attribute>
	    <xsl:attribute name="height">24</xsl:attribute>
	</xsl:element>
    </xsl:template>

    <xsl:template match="text()">
    </xsl:template>

</xsl:stylesheet>