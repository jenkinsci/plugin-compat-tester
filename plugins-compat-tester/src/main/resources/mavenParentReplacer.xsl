<!--
 *
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
-->
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
