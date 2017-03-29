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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="html" omit-xml-declaration="no" indent="yes"/>

    <xsl:variable name="testedJenkinsCoordinates" select="/report/testedCoreCoordinates/coord[groupId='org.jenkins-ci.plugins']" />
    <xsl:variable name="testedHudsonCoordinates" select="/report/testedCoreCoordinates/coord[groupId='org.jvnet.hudson.plugins']" />
    <xsl:variable name="otherCoreCoordinates" select="/report/testedCoreCoordinates/coord[not(groupId='org.jenkins-ci.plugins') and not(groupId='org.jvnet.hudson.plugins')]" />

    <xsl:template match="/">
<html>
    <head>
        <title>Plugin compat tester report</title>
        <link href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css" rel="stylesheet" type="text/css"/>
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.5/jquery.min.js"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js"></script>
        <script type="text/javascript">
            var latestDialog = null;
        </script>

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
                <xsl:if test="count($testedJenkinsCoordinates) &gt; 0">
                    <xsl:element name="th">
                        <xsl:attribute name="colspan"><xsl:value-of select="count($testedJenkinsCoordinates)" /></xsl:attribute>
                        Jenkins core
                    </xsl:element>
                </xsl:if>
                <xsl:if test="count($testedHudsonCoordinates) &gt; 0">
                    <xsl:element name="th">
                        <xsl:attribute name="colspan"><xsl:value-of select="count($testedHudsonCoordinates)" /></xsl:attribute>
                        Hudson core
                    </xsl:element>
                </xsl:if>
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
                <xsl:for-each select="$testedHudsonCoordinates" >
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
                 <xsl:variable name="compatResult" select="$currentEntry/list/compatResult[./coreCoordinates/version = current()/version and ./coreCoordinates/groupId = current()/groupId and ./coreCoordinates/artifactId = current()/artifactId]" />
                 <xsl:call-template name="display-cell">
                     <xsl:with-param name="compatResult" select="$compatResult" />
                     <xsl:with-param name="pluginInfos" select="$compatResult/../../pluginInfos" />
                 </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$testedHudsonCoordinates">
                 <xsl:variable name="compatResult" select="$currentEntry/list/compatResult[./coreCoordinates/version = current()/version and ./coreCoordinates/groupId = current()/groupId and ./coreCoordinates/artifactId = current()/artifactId]" />
                 <xsl:call-template name="display-cell">
                     <xsl:with-param name="compatResult" select="$compatResult" />
                     <xsl:with-param name="pluginInfos" select="$compatResult/../../pluginInfos" />
                   </xsl:call-template>
            </xsl:for-each>
           <xsl:for-each select="$otherCoreCoordinates">
                <xsl:variable name="compatResult" select="$currentEntry/list/compatResult[./coreCoordinates/version = current()/version and ./coreCoordinates/groupId = current()/groupId and ./coreCoordinates/artifactId = current()/artifactId]" />
                <xsl:call-template name="display-cell">
                    <xsl:with-param name="compatResult" select="$compatResult" />
                    <xsl:with-param name="pluginInfos" select="$compatResult/../../pluginInfos" />
                </xsl:call-template>
           </xsl:for-each>
        </tr>
    </xsl:template>

    <xsl:template name="display-cell">
        <xsl:param name="compatResult" />
        <xsl:param name="pluginInfos" />
        <xsl:variable name="cellId"><xsl:value-of select="$pluginInfos/pluginName" />-<xsl:value-of select="translate($pluginInfos/pluginVersion, '.', '_')" />_<xsl:value-of select="translate($compatResult/coreCoordinates/groupId, '.', '_')" />_<xsl:value-of select="$compatResult/coreCoordinates/artifactId" />_<xsl:value-of select="translate($compatResult/coreCoordinates/version, '.', '_')" /></xsl:variable>
        <xsl:element name="td">
	  <xsl:attribute name="id"><xsl:value-of select="$cellId" /></xsl:attribute>
            <xsl:choose>
                <xsl:when test="count($compatResult)=0">-----</xsl:when>
                <xsl:otherwise>
		<xsl:choose>
			<xsl:when test="$compatResult/status = 'INTERNAL_ERROR'"><xsl:call-template name="display-internal-error"><xsl:with-param name="id" select="$cellId" /><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'COMPILATION_ERROR'"><xsl:call-template name="display-compilation-error"><xsl:with-param name="id" select="$cellId" /><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'TEST_FAILURES'"><xsl:call-template name="display-tests-failures"><xsl:with-param name="id" select="$cellId" /><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
			<xsl:when test="$compatResult/status = 'SUCCESS'"><xsl:call-template name="display-success"><xsl:with-param name="id" select="$cellId" /><xsl:with-param name="compatResult" select="$compatResult" /></xsl:call-template></xsl:when>
		</xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="count($compatResult/warningMessages//string) &gt; 0">
                <xsl:call-template name="display-img">
                    <xsl:with-param name="id"><xsl:value-of select="$cellId"/>-warns</xsl:with-param>
                    <xsl:with-param name="title">Warnings !</xsl:with-param>
                    <xsl:with-param name="img">document.png</xsl:with-param>
                    <xsl:with-param name="error"><xsl:value-of select="$compatResult/warningMessages//string" /></xsl:with-param>
                </xsl:call-template>
            </xsl:if>
            <xsl:if test="$compatResult/buildLogPath != ''">
                <xsl:element name="a">
                    <xsl:attribute name="href"><xsl:value-of select="$compatResult/buildLogPath" /></xsl:attribute>
                    <xsl:call-template name="display-img">
                        <xsl:with-param name="id"><xsl:value-of select="$cellId"/>-logs</xsl:with-param>
                        <xsl:with-param name="title">Logs</xsl:with-param>
                        <xsl:with-param name="img">terminal.png</xsl:with-param>
                        <xsl:with-param name="error">_</xsl:with-param>
                    </xsl:call-template>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <xsl:template name="display-internal-error">
	<xsl:param name="id" />
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="id" select="$id" />
		<xsl:with-param name="compilation-title">Compilation : Internal error !</xsl:with-param>
		<xsl:with-param name="compilation-img">red.png</xsl:with-param>
		<xsl:with-param name="compilation-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
		<xsl:with-param name="tests-title">Tests : Internal error !</xsl:with-param>
		<xsl:with-param name="tests-img">red.png</xsl:with-param>
		<xsl:with-param name="tests-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-compilation-error">
	<xsl:param name="id" />
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="id" select="$id" />
		<xsl:with-param name="compilation-title">Compilation : failure !</xsl:with-param>
		<xsl:with-param name="compilation-img">yellow.png</xsl:with-param>
		<xsl:with-param name="compilation-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
		<xsl:with-param name="tests-title">Tests : No tests executed !</xsl:with-param>
		<xsl:with-param name="tests-img">red.png</xsl:with-param>
		<xsl:with-param name="tests-error">Compilation failed => No tests executed !</xsl:with-param>
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-tests-failures">
	<xsl:param name="id" />
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="id" select="$id" />
		<xsl:with-param name="compilation-title">Compilation : Success !</xsl:with-param>
		<xsl:with-param name="compilation-img">blue.png</xsl:with-param>
		<xsl:with-param name="compilation-error">_</xsl:with-param>
		<xsl:with-param name="tests-title">Tests : Some tests are in failure !</xsl:with-param>
		<xsl:with-param name="tests-img">yellow.png</xsl:with-param>
		<xsl:with-param name="tests-error"><xsl:value-of select="$compatResult/errorMessage" /></xsl:with-param>
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-success">
	<xsl:param name="id" />
	<xsl:param name="compatResult" />
	<xsl:call-template name="display-result">
		<xsl:with-param name="id" select="$id" />
		<xsl:with-param name="compilation-title">Compilation : Success !</xsl:with-param>
		<xsl:with-param name="compilation-img">blue.png</xsl:with-param>
		<xsl:with-param name="compilation-error">_</xsl:with-param>
		<xsl:with-param name="tests-title">Tests : Success !</xsl:with-param>
		<xsl:with-param name="tests-img">blue.png</xsl:with-param>
		<xsl:with-param name="tests-error">_</xsl:with-param>
	</xsl:call-template>
    </xsl:template>

    <xsl:template name="display-result">
        <xsl:param name="id" />
        <xsl:param name="compilation-title"/>
        <xsl:param name="compilation-img" />
        <xsl:param name="compilation-error" />
        <xsl:param name="tests-title" />
        <xsl:param name="tests-img" />
        <xsl:param name="tests-error" />
        <xsl:call-template name="display-img">
            <xsl:with-param name="id"><xsl:value-of select="$id"/>-compilation</xsl:with-param>
            <xsl:with-param name="title" select="$compilation-title" />
            <xsl:with-param name="img" select="$compilation-img" />
            <xsl:with-param name="error" select="$compilation-error" />
        </xsl:call-template>
        <xsl:call-template name="display-img">
            <xsl:with-param name="id"><xsl:value-of select="$id"/>-tests</xsl:with-param>
            <xsl:with-param name="title" select="$tests-title" />
            <xsl:with-param name="img" select="$tests-img" />
            <xsl:with-param name="error" select="$tests-error" />
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="display-img">
        <xsl:param name="id" />
        <xsl:param name="title" />
        <xsl:param name="img" />
        <xsl:param name="error" />

	<xsl:element name="img">
	    <xsl:attribute name="id"><xsl:value-of select="$id" /></xsl:attribute>
	    <xsl:attribute name="alt"><xsl:value-of select="$title" /></xsl:attribute>
	    <xsl:attribute name="title"><xsl:value-of select="$title" /><xsl:if test="not($error='_')"><xsl:value-of select="$error" /></xsl:if></xsl:attribute>
	    <xsl:choose>
	        <xsl:when test="$img='blue.png'">
	            <xsl:attribute name="src">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAESUlEQVR4AdWSA/DkyANGX3cy3J+5tm3btq3C37bNs+8KZ9u2tbZtDDJJJrpMKrd7Nl/VF+d7SXfzrUflYxg69M/q7mSkY1l56bgWDasGxROR+NZdx9acPJW6N57WXwOyfASiw9A/82FE65cvmjqs09+aNKqqLSlJxvOOIGc6KEKSzmh26lT61IPPbrwpuuvQzwD7AwVDP0CwV42Xduve5Po547sOlxGZtB0bBFiO5ws80jmPU5pLzrBoVhFx3ly7Y8Nzq/YsBNa9T9B8yrm8k53d0nIpnV6bNrZrdz1vUJwUJKMCAZg2pHWX03758UwQTp3WqEhIrGzq4J3PrO0FHOQdqJZn8U4G7qv/r/GLO3U+ndOpKpFUFkmK/QIJaHkPKcB2QM97aIZAi6rsPZ6hSWVR/a7N690CDHqXwL9ICFv2HW07fXTXlcdzllpRJ/hySvzyqmKJIuFk1sW0BDEDoqpA9aMohXuSNTtPMqhri963PfTGL4D/nRHsOXSaEOpVlP+xvKqyIp3TQCgIAdJPRAFVEUgZnCOEAEAEERCev7r5aLR3p0aL3yXwLxCClGrzIykTVYJle5gWaKbHiawbFGuGh2F5wT3bAdv1cFwX14/n5/ApjfYNaqvvf/aFKJAPBC+tWUMIM8cMr3f0tEFFsRIUp/WgOBgWISiUByvIFwVzYPqxLNuPg+X4EsAVsiytJToDrwcC/wSAmOdUR1SlKJs1iagJoqqHFG7wpVkDCgbLDso5nXPJGC4508YwLV9kkfcD4CFjUUX2PyPwTwDYft8vjxtTn8rphqxQFYkUMWxHoFseUUUgzq6eoDylOWiaQS5nYhh5X2ADIDwnb7vm64So/gkATSb8w9t3uMPheHF5w3RWx/U8TDuGZopgkgsGxwnmAN2w0XIG2axR2PsxIUQKL+Ukk6sIEQ1nnQshDZLJ2wb36zBz84EMdZIxEvEo0YiKoioICCa0MN6mmSen54PyTFY/8/XVZQkalUU2Ae0JUcd3bQkhT76+5dy4dEaZulFaGFM9FiUSUYK1LoTADQWFQt0oSExs24GQfu3q2nc/tup+3oHoPu9i3okSkVeNHNBhxRNv7hexaCQQSPm2oLA8A0Gwet5J5xbVZE6dWH1bYmsPwCVE8Oc/804aPrIhMWDI9DWVNdWtXt10mE9Co5pimlfHTt50ywMjD7500WrAOyt4NwmgpLj50FYDxy64rHf3tm2fX39YTWkmH4QiBQM61ef0ieOHHnrgvr8cevXaR4AMkALsDxIUFwRhiluM+tl3J40ZNlVJJEtsR4iDx7OYlkOj6iKKEirCyRtPPr9q1Rv3/v9vTu7YUSD9juh8ABIoBxoB7YCeycqWU2q7zftn+4m/e3zKD6/YOOeX12zrPvNvL9Trs+zy4gbdlwIDgW5AK6A+UMQnQAkfrAEaAy1DYccwHYC2QAugIVAVDq/gcyL8yGD/TeEt6MoM8GzBZF0AAAAASUVORK5CYII=</xsl:attribute>
	        </xsl:when>
	        <xsl:when test="$img='red.png'">
	            <xsl:attribute name="src">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAD20lEQVR4AcSVA9AjTxDFX29sJ5+/v23btm3btm3btm3jbNtmnJnrrq05Zi+FQ1f9amf1XmNBWmssz7CYlWtwK5H7LKJNbstmr31+552/en2ffX66u63t4Ys9nl3PJwqjUXCLHLk+lTrxm4suGjH4iScK0994Q5dfekmXnnhCz3ruOT3s3nsrf1555aT7V1nl0VsAt5NG3RmcTRTbdIcd3tj3tNN2a1EqqMtlgAioVqELBWDOHKhZs6CLReRXXbX246+/9v33hx9OeKhU6t2wRdwSa7t99/3pxPPOO6C5WAzC64WVStkkErBiMVAkAgqHQS4XAj16uA5YddWN9j/uuG9OJ2rBYuHGYuFfZZW79zvmmI08kyaBkkkRBEIhuwLJ3rKgazVQqQT4fCBOQI0cia1WWaVl5FZbvQtgR8cKrotG19n91FPPiE6c6JbMiQVY3M6czSRr+P0QUXg8gNttw5Xonj2x5x57bHVHMnmlo0G6ufmmDXK5JDg7yVggyxIBW5DXgjkHE7LWGrH//vNuuOuuJzkarLLhhquBWwNuASoVCDJUPXMm9PTpkLWY83EZuH2dUjBbNW4c0tls5igib90ZZDo6mvWECYD0nZ8QzU+LZGeeIhHnY2Ik5+W4DZsJUkWSKJ4ENgLQZRGD04gyF199dVhPnQrpKzFaxGWg+bxtYCqaPRuajxkjMQYbSSSIfEmfb7slDF4GppxaKORVPp+0WFxJ/7lskqyl/xJsJoIiLiZSjZK1ICYcs5QqzyyVuizRIs3xycEHT6BYrE16DhaXjCHZs6FUIAbSEslcxI2JGJiYDMz0Ad3rDnlEz54jwI+kYgPFQ1XTpkFNnWozZYpZ2+dmzJC32TaRREQsm8X0qVOnPKR1oa7B2LFjHxrq8czkDOuLy5aPa0EMGGmNifx221X7/P77F46P6f2Vyt/fv/vu+8Utt9TGgEUNxlBMpEr7UTVCm2yCH/76q09pwoRrlvqp+HDMmIuae/feef+tt15T//03pJpGYXV0oHc4PO3LX3897R1A3+xUAREF/gaiD/3442lv9erVZ86BB1YpHodjuFyo7bILfggExj/0yivXsfh0AEnWMYmbzzWMQQRsYIPIudHoeUccccQha/l80ZhSxEOSnkvGmMvfqNG1WvGrr77q/uDw4bfPAiYBmGXQZtC8MJiKEkw7sy6zRQtw8H6WddflqdQPz+24Y7/X9t578E0dHX8e5fE8syZwCoAdmE2ZNZkWJtzwh8OVuAAEmCDjZ7yMh5HjEpqpMRWmxBSBeWP4CqL/oxlIUquCEQhAFNjjRGqkebMFAFqsVfjQMePDAAAAAElFTkSuQmCC</xsl:attribute>
	        </xsl:when>
	        <xsl:when test="$img='yellow.png'">
	            <xsl:attribute name="src">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAD/UlEQVR4AcSVA5AsSRRFb2ZV2/oerG3btu0NrI3A2rZt2za/batj2Cgr90VGz3K0fhGnb7XuKRcTQuDfnP9fcMUVTB0RT6ydyxf2yBdW2yYcjkW72qdMrHS2valFtdGnniq0vyx46aFRx6y2zuFXlYauNDSVykQjIRsiMOAFYejVmlfp7uqePe3150bPm3beZZcJb9CCB2/IZ1ZcdYOn19vkmB2zKT8uhAOAAdQhhAkEGgK/BmY78LyV/Kkzvp06Z977Rx91ujZ5QAHtEr7jpvuP3njzQzdUWAWMJ8FYBPQCkEgEOgSVB36Vshtcq0Fxi5hdNpd9N/rJTU68UF+GXw3H72aDFde4br0ND1xXQZss50oGTC2AKQTPEimAJyijUizUEIS7CKsUUiNWad3kBdD0KXju7uLqa29w1MkRtayChWUBlZEkR+RJkgQYFbMwZYhQAYUquAKmTcL6q+686dsPNl3QpyCXG3HpiBHFPGADYHK3MHBKRRZCLhNgDSCzZzFVHRduatnu2D4FxaFrrQi/DRA+4UqEMCEa+1sEJhDYje88AD4QBBImKM0ykrFS6aUrWLhXQSrdMly4ZSq1CYsKNVkeeF0I/E5arsjPSAoIi9IB8zwJfJJBIBXi2Xo6vu4fBI/dlSrFw5Ektw15piCoN86WCiWVe4TcihplndIAcy0wx6Z0JXIlVR5JquEt0RgVjTnhDK1j9neGwaJOnik6gggDEwFhQ8j9j8aus2S58DRw0wS3SWLbPwsMP3B01x7zewGNEPXKoWUllR0FvY4gEBBRF6AyebaAET6VO3LNuUXlhgFGEmZZ6JmKG1STzBzfiwDobJu2gI/ae2Ohl8F8KnMciFAIQlEgRwTgrkcCB5xKWY/E9yAnUoJudHYcco4wexVUupffWjaVXYbZbiZwXYiwDaGqAOcAY/JsIbEUyN1CEnqPnjGyG3uLxj7+9rp9naaHnN353cSpr7xkJTYQSr0GpVb9hapMcEkNXNd/Ux5k18GEBd9PGbt0/sX93io+fG/mmRMXTp4T5DaSa0lF4PU6QaWaPLDytPzNxEdhrh3t+uSzL068/HKIPm92jLEYRXqf7dVVzjhxy/s2X3P71VOVH1S4VfQ6TIFT2AITly1d/uKbb11x8yPaBwDqRJV6vd4EKZCgQermi4ecusdOB+3fnI6nE1wwZi0HfBuIj4TJE2gzXeubMe+OP+/a6VeV29EGoNYD9Zq9CThFhkg2SKy5mjJyx00jm629cn7T1pY1RoQj8dDiJTPaZsxdOvmLseb334725wDQCQ2UjXKt3wcOiRSKGBEnokSYCBEK5EAQPuESNmE1yi1B85cf+oyGgoAQPw0IBkWrAgBmODtGM+C21QAAAABJRU5ErkJggg==</xsl:attribute>
	        </xsl:when>
            <xsl:when test="$img='document.png'">
                <xsl:attribute name="src">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAQAAABKfvVzAAABNklEQVR4XpWRMU4zQQyFP89ONvmlUMIfKQUXoKfnCtBzAG7CBTgANYoUUdFyA46BRIcSkp0ZIyE0luUKr+xm/Om9583K3yoDwOPl7CYDjSONiUKjUig/c6Jw3N6/GsDF0/laSXg9tcnbLWcGMOZPCiMNQaC31XLmLCny+30EQDlFUBxAB+gT/FtUINmjQ5Kx0VKKGZBoqdHMEngNu54BsOefN+QUgqXKCRVibIMd0A2pqXhLNA8A3gxLlghYXA9oN7FCSF3FkHClHTMA3v06oKwioGQGFxPEZRC8AnOqrYLTEDRkcKvxnBL/QyxQ1ggNjZZM8j8J6a32igd6i7Utxyvt2hxlABfXEsBXc8Dz3cs1Mi3qoox1bFkHSC2VNOVjPoz7VA/bKwPgYcOm30YYGBAqVRu++Abw8GiSNi5W4wAAAABJRU5ErkJggg==</xsl:attribute>
            </xsl:when>
            <xsl:when test="$img='terminal.png'">
	            <xsl:attribute name="src">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAADyklEQVR4AbSUA4w0WRDHfz3b823v2bZt27btu+Bs246Ts23b1uJs8/O49aqup9LXmfRZlVTq5eGPqp7h/w6fPG6+7bp1fN/fFejjd0Kc4MRlKUgqOHW2Jy7fc7bnOu32TcDTAN55F57Jssss++qcc8yzZLU6KuCfhoKiqCq1ei0cHh4cAVbyusqXW2alh2q1WjA08mZ2OIE88LpZqVDxPIqa5R/tTTrJ5Mw7z4IodO65547N/Cx27SofHH6D62+4hjAMUQDVQpqSV7Xao7h0RyEIAo487BiWXHyZgYrft6tvPUdNebPV5PAT90HFrKI4VEAMWYqqorZWlZxYkXx9+aV30eo0CKOIOI77/N4eOudot9o2MA8pQEToBYaeM833raqSpilOlCgMiaMYX5wUHXepo9lsIAqqxcNCnYqB9BBrlo5O1KITtmh3WrTaDSR1hFFGEEf4ScYI2KCcS2k0WvgDo0iSBImzNKIc0NRipI1mjWa7ThSHduYVn7IJsllGcYKfpklOUCHpOmg0mGX6udl6kz14bfg5ht56hXa9iXpiQK12k3pjAqlLoIAFz5aYK5cKnU7HHPtxxpKm9kOx/tUyB+7rrxn56E2WW3xV1lhxA15661meePwevvvqKww4hy6je+ZASdKEMOwgXYIkjummOjWiRr1BfeJEbv3uKmaeZw6WX2FVVlt+XV4bfJ5P3v+QMm55Q8RwzIGo4EcZeJzENqwkThj902hmXXAuTjvmQqaYbEqeePlhTjj1IL744GNwmussl2JhDtIkxQjECMwBzokNtlZrME0n4uFn7+ORB+5lzLc/oqIFRgmxtAdirU7sK1JV/CT6xYEYexylfDLyIR+9+T4epR54lHtfdmEYIhgegB8lkbVGnNhmHDqIXC8mcy8xP3vvfQBer2q1f2A+eP3d8ldkyguCOHfgehyUVY/7YRwvv/wi5Rj9/Zje+ybACIQegnwGlYrHZJNNxpjv6pQim0Od91/9lL8Sk006qTnsJXDZoG0w+++/vynwPC8jrHSr5Z+E/X6iKCT7V2YgCOiEbfyqD+D8ThjelIHv4pwLyEMVRPhLscjOMUPXKv39/QRBv4EDtNutZoZ5k7fjrtsx95xzv7roIosuiecFIg4VGxKWarVIOyvtudTh+76pbjYbGXi7+ePonwaBNb19DtgTgFartU7Wlr1UNFCVqipZqp9lVVEfpS+rffkwBXBZ+9IsE8/Dap6hE7kWeArA22GXbSnHpx9/6gOjgAEgyGsVqBg+CJACnTxDIAISSuH9PCMTfQZaAgDebA8VxUHAhwAAAABJRU5ErkJggg==</xsl:attribute>
	        </xsl:when>
	    </xsl:choose>
	    <xsl:attribute name="width">24</xsl:attribute>
	    <xsl:attribute name="height">24</xsl:attribute>
	    <xsl:if test="not($error='_')">
		<xsl:attribute name="onclick">if(latestDialog!=null){ $(latestDialog).dialog('close'); } latestDialog="#<xsl:value-of select="$id" />_dialog"; $("#<xsl:value-of select="$id" />_dialog").dialog();</xsl:attribute>
	    </xsl:if>
	</xsl:element>
	<xsl:if test="not($error='_')">
		<xsl:element name="span">
		    <xsl:attribute name="id"><xsl:value-of select="$id" />_dialog</xsl:attribute>
		    <xsl:attribute name="title"><xsl:value-of select="$title" /></xsl:attribute>
		    <xsl:attribute name="style">display:none</xsl:attribute>
		    <pre><xsl:value-of select="$error" /></pre>
		</xsl:element>
	</xsl:if>
    </xsl:template>

    <xsl:template match="text()">
    </xsl:template>

</xsl:stylesheet>