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
            <xsl:apply-templates select="/report/pluginCompatTests/entry" />
        </table>
    </body>
</html>
    </xsl:template>

    <xsl:template match="/report/pluginCompatTests/entry">
        <xsl:variable name="currentEntry" select="." />
        <tr>
           <td><xsl:value-of select="pluginInfos/pluginName/text()"/></td>
           <td><xsl:value-of select="pluginInfos/pluginVersion/text()"/></td>
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
                    <xsl:call-template name="display-result">
                        <xsl:with-param name="result"><xsl:value-of select="$compatResult/compilationOk" /></xsl:with-param>
                        <xsl:with-param name="title">Compilation result</xsl:with-param>
                        <xsl:with-param name="error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
                    </xsl:call-template>
                    <xsl:call-template name="display-result">
                        <xsl:with-param name="result"><xsl:value-of select="$compatResult/testsOk" /></xsl:with-param>
                        <xsl:with-param name="title">Tests result</xsl:with-param>
                        <xsl:with-param name="error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </td>
    </xsl:template>

    <xsl:template name="display-result">
        <xsl:param name="result" />
        <xsl:param name="title" />
        <xsl:param name="error" />

        <xsl:choose>
            <xsl:when test="$result = 'true'">
                <xsl:element name="img">
                    <xsl:attribute name="alt"><xsl:value-of select="$title" /> : Success</xsl:attribute>
                    <xsl:attribute name="title"><xsl:value-of select="$title" /> : Success</xsl:attribute>
                    <xsl:attribute name="src">https://github.com/jenkinsci/jenkins/raw/master/war/src/main/webapp/images/24x24/blue.png</xsl:attribute>
                    <xsl:attribute name="width">24</xsl:attribute>
                    <xsl:attribute name="height">24</xsl:attribute>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="img">
                    <xsl:attribute name="alt"><xsl:value-of select="$title" /> : Failure => <xsl:value-of select="$error" /></xsl:attribute>
                    <xsl:attribute name="title"><xsl:value-of select="$title" /> : Failure => <xsl:value-of select="$error" /></xsl:attribute>
                    <xsl:attribute name="src">https://github.com/jenkinsci/jenkins/raw/master/war/src/main/webapp/images/24x24/red.png</xsl:attribute>
                    <xsl:attribute name="width">24</xsl:attribute>
                    <xsl:attribute name="height">24</xsl:attribute>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="text()">
    </xsl:template>

</xsl:stylesheet>