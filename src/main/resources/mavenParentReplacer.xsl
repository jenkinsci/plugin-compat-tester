<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:mvn="http://maven.apache.org/POM/4.0.0" version="1.0">
	<xsl:namespace-alias result-prefix="#default" stylesheet-prefix="mvn"/>
	<xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>
	
	<xsl:param name="parentArtifactId" />
	<xsl:param name="parentGroupId" />
	<xsl:param name="parentVersion" />

 	<xsl:template match="/">
 		<xsl:apply-templates />
 	</xsl:template>
 	
 	<xsl:template match="/mvn:project/mvn:parent/mvn:artifactId">
 		<artifactId><xsl:value-of select="$parentArtifactId" /></artifactId>
 	</xsl:template>
 	
 	<xsl:template match="/mvn:project/mvn:parent/mvn:groupId">
 		<groupId><xsl:value-of select="$parentGroupId" /></groupId>
 	</xsl:template>
 	
 	<xsl:template match="/mvn:project/mvn:parent/mvn:version">
 		<version><xsl:value-of select="$parentVersion" /></version>
 	</xsl:template>
 	
 	<!-- Recopying everything... -->
 	<xsl:template match="@*|node()">
 		<xsl:copy>
 			<xsl:apply-templates select="@*|node()" />
 		</xsl:copy>
 	</xsl:template>
 	
</xsl:stylesheet>
