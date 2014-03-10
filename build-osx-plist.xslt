<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- The output format is an Apple plist file -->
    <xsl:output method="xml" indent="yes" doctype-public="-//Apple//DTD PLIST 1.0//EN" doctype-system="http://www.apple.com/DTDs/PropertyList-1.0.dtd" />

    <!-- Copy all XML exactly the same unless it matches a template below this one -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Add extra keys to the end of the 'dict' element -->
    <xsl:template match="dict">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
            <key>NSHighResolutionCapable</key>
            <true/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>