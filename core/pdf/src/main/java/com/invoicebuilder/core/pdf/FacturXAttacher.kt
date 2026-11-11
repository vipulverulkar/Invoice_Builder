package com.invoicebuilder.core.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentNameDictionary
import com.tom_roush.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode
import com.tom_roush.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification
import com.tom_roush.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar

object FacturXAttacher {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Embeds a UBL / Factur-X XML byte array into an existing PDF document.
     */
    fun attachXml(
        pdfBytes: ByteArray,
        xmlBytes: ByteArray,
        attachmentName: String = "ubl-invoice.xml"
    ): ByteArray {
        val document = PDDocument.load(pdfBytes)
        val catalog = document.documentCatalog

        val embeddedFile = PDEmbeddedFile(document, ByteArrayInputStream(xmlBytes)).apply {
            subtype = "application/xml"
            size = xmlBytes.size
            modDate = Calendar.getInstance()
        }

        val fileSpec = PDComplexFileSpecification().apply {
            file = attachmentName
            setEmbeddedFile(embeddedFile)
        }

        val names = catalog.names ?: PDDocumentNameDictionary(catalog)
        val embeddedFiles = names.embeddedFiles ?: PDEmbeddedFilesNameTreeNode()

        val currentNames = embeddedFiles.names?.toMutableMap() ?: mutableMapOf()
        currentNames[attachmentName] = fileSpec
        embeddedFiles.names = currentNames
        names.embeddedFiles = embeddedFiles
        catalog.names = names

        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()

        return output.toByteArray()
    }
}
