/**

 The ISAconverter, ISAvalidator & BII Management Tool are components of the ISA software suite (http://www.isa-tools.org)

 Exhibit A
 The ISAconverter, ISAvalidator & BII Management Tool are licensed under the Mozilla Public License (MPL) version
 1.1/GPL version 2.0/LGPL version 2.1

 "The contents of this file are subject to the Mozilla Public License
 Version 1.1 (the "License"). You may not use this file except in compliance with the License.
 You may obtain copies of the Licenses at http://www.mozilla.org/MPL/MPL-1.1.html.

 Software distributed under the License is distributed on an "AS IS"
 basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 License for the specific language governing rights and limitations
 under the License.                                    5

 The Original Code is the ISAconverter, ISAvalidator & BII Management Tool.

 The Initial Developer of the Original Code is the ISA Team (Eamonn Maguire, eamonnmag@gmail.com;
 Philippe Rocca-Serra, proccaserra@gmail.com; Susanna-Assunta Sansone, sa.sanson@gmail.com;
 http://www.isa-tools.org). All portions of the code written by the ISA Team are Copyright (c)
 2007-2011 ISA Team. All Rights Reserved.

 Contributor(s):
 Rocca-Serra P, Brandizi M, Maguire E, Sklyar N, Taylor C, Begley K, Field D,
 Harris S, Hide W, Hofmann O, Neumann S, Sterk P, Tong W, Sansone SA. ISA software suite:
 supporting standards-compliant experimental annotation and enabling curation at the community level.
 Bioinformatics 2010;26(18):2354-6.

 Alternatively, the contents of this file may be used under the terms of either the GNU General
 Public License Version 2 or later (the "GPL") - http://www.gnu.org/licenses/gpl-2.0.html, or
 the GNU Lesser General Public License Version 2.1 or later (the "LGPL") -
 http://www.gnu.org/licenses/lgpl-2.1.html, in which case the provisions of the GPL
 or the LGPL are applicable instead of those above. If you wish to allow use of your version
 of this file only under the terms of either the GPL or the LGPL, and not to allow others to
 use your version of this file under the terms of the MPL, indicate your decision by deleting
 the provisions above and replace them with the notice and other provisions required by the
 GPL or the LGPL. If you do not delete the provisions above, a recipient may use your version
 of this file under the terms of any one of the MPL, the GPL or the LGPL.

 Sponsors:
 The ISA Team and the ISA software suite have been funded by the EU Carcinogenomics project
 (http://www.carcinogenomics.eu), the UK BBSRC (http://www.bbsrc.ac.uk), the UK NERC-NEBC
 (http://nebc.nerc.ac.uk) and in part by the EU NuGO consortium (http://www.nugo.org/everyone).

 */

package org.isatools.isatab.export.sra;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
//testing to see whether it pulls md5
import org.isatools.isatab.export.sra.SraExportUtils;
import org.isatools.isatab.export.sra.templateutil.*;
import org.isatools.tablib.exceptions.TabInvalidValueException;
import org.isatools.tablib.exceptions.TabMissingValueException;
import org.isatools.tablib.utils.BIIObjectStore;
import uk.ac.ebi.bioinvindex.model.*;
import uk.ac.ebi.bioinvindex.model.processing.Assay;
import uk.ac.ebi.bioinvindex.model.processing.ProtocolApplication;
import uk.ac.ebi.bioinvindex.model.term.OntologyTerm;
import uk.ac.ebi.bioinvindex.model.term.ProtocolComponent;
import uk.ac.ebi.bioinvindex.model.xref.Xref;
import uk.ac.ebi.bioinvindex.utils.i18n;
import uk.ac.ebi.bioinvindex.utils.processing.ProcessingUtils;

import uk.ac.ebi.embl.era.sra.xml.*;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.LIBRARYDESCRIPTOR;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.LIBRARYDESCRIPTOR.LIBRARYLAYOUT;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.LIBRARYDESCRIPTOR.LIBRARYSELECTION;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.LIBRARYDESCRIPTOR.LIBRARYSOURCE;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.LIBRARYDESCRIPTOR.LIBRARYSTRATEGY;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.DESIGN.SAMPLEDESCRIPTOR;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.PROCESSING;
import uk.ac.ebi.embl.era.sra.xml.ExperimentType.STUDYREF;

import uk.ac.ebi.embl.era.sra.xml.RunType.DATABLOCK;
import uk.ac.ebi.embl.era.sra.xml.RunType.DATABLOCK.FILES;
import uk.ac.ebi.embl.era.sra.xml.RunType.DATABLOCK.FILES.FILE;
import uk.ac.ebi.embl.era.sra.xml.RunType.DATABLOCK.FILES.FILE.ChecksumMethod;
import uk.ac.ebi.embl.era.sra.xml.RunType.EXPERIMENTREF;

import java.io.File;

import uk.ac.ebi.utils.io.IOUtils;
import org.isatools.tablib.exceptions.TabIOException;
import org.isatools.tablib.exceptions.TabInternalErrorException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.*;

/**
 * SRA-exporter, functions related to the ISATAB experimental pipeline. See {@link SraExportComponent} for further
 * information on how the SRA exporter classes are arranged.
 *
 * @author brandizi
 *         <b>date</b>: Jul 20, 2009
 */
abstract class SraExportPipelineComponent extends SraExportSampleComponent {

    protected static SRATemplateLoader sraTemplateLoader = new SRATemplateLoader();

    protected SraExportPipelineComponent(BIIObjectStore store, String sourcePath, String exportPath) {
        super(store, sourcePath, exportPath);
    }

    private boolean containsAnnotation(Assay assay, String annotation) {
        Collection<Xref> crossReferences = assay.getXrefs();

        for (Xref xref : crossReferences) {
            log.info("Found XREF for " + xref.getAcc());
            if (xref.getSource().getAcc().contains(annotation)) {

                return true;
            }
        }

        return false;
    }


    /**
     * Builds the SRA elements that are related to this ISATAB assay. It adds runs and an experiment to the respective
     * set.
     *
     * @return true if it could successfully build the exported items.
     */
    protected boolean buildExportedAssay(
            Assay assay, SubmissionType.FILES xsubFiles, RunSetType xrunSet, ExperimentSetType xexperimentSet, SampleSetType xsampleSet) {


        String assayAcc = assay.getAcc();

        boolean doExport = true;
//
        if (containsAnnotation(assay, "EXPORT")) {

            log.info("HAS EXPORT COMMENT IN ASSAY");

            String export = assay.getSingleAnnotationValue("comment:Export");

            log.info("export is " + export);
            if (export.equalsIgnoreCase("no")) {
                doExport = false;
            } else {
                doExport = true;
            }
//            doExport = !(export != null && export.toLowerCase().contains("yes"));

        } else {
            log.info("NO EXPORT COMMENT FOUND");
        }

        log.info("Perform export? " + doExport);

        if (doExport) {

            // Now create an experiment for the input material and link it to the run

            // get Material associated to the assay and get its identifier
            Material material = assay.getMaterial();
            String materialAcc = material.getAcc();

            //create a new SRA Experiment and assign ISA Material name as SRA Experiment Title
            ExperimentType xexp = ExperimentType.Factory.newInstance();
            xexp.setAlias(materialAcc);
            xexp.setTITLE("Sequencing library derived from sample " + material.getName());

            xexp.setCenterName(centerName);
            xexp.setBrokerName(brokerName);

            PlatformType xplatform = buildExportedPlatform(assay);
            if (xplatform == null) {
                return false;
            }
            xexp.setPLATFORM(xplatform);

            Map<SequencingProperties, String> sequencingProperties = getSequencingInstrumentAndLayout(assay);

            xexp.setPROCESSING(buildExportedProcessing(assay, sequencingProperties));


            STUDYREF xstudyRef = STUDYREF.Factory.newInstance();
            xstudyRef.setRefname(assay.getStudy().getAcc());
            xexp.setSTUDYREF(xstudyRef);
            EXPERIMENTREF xexpRef = EXPERIMENTREF.Factory.newInstance();
            xexpRef.setRefname(materialAcc);

            DESIGN xdesign = DESIGN.Factory.newInstance();
            xdesign.setDESIGNDESCRIPTION("See study and sample descriptions for details");

            SAMPLEDESCRIPTOR xsampleRef = buildExportedAssaySample(assay, xsampleSet);
            if (xsampleRef == null) {
                return false;
            }

            xdesign.setSAMPLEDESCRIPTOR(xsampleRef);

            LIBRARYDESCRIPTOR xlib = buildExportedLibraryDescriptor(assay);
            if (xlib == null) {
                return false;
            }
            xdesign.setLIBRARYDESCRIPTOR(xlib);

            SpotDescriptorType xspotd = buildExportedSpotDescriptor(assay, sequencingProperties);
            if (xspotd == null) {
                return false;
            }

            xdesign.setSPOTDESCRIPTOR(xspotd);

            xexp.setDESIGN(xdesign);

            Map<String, String> fileToMD5 = new HashMap<String, String>();

            // For each file, builds one run, with one data block and one file
            // TODO: We should introduce something like "Run Name", so that multiple files associated to a single run can be
            // specified
            //
            for (AssayResult ar : ProcessingUtils.findAssayResultsFromAssay(assay)) {
                Data data = ar.getData();
                String url = StringUtils.trimToNull(data.getUrl());

                Study study = ar.getStudy();

                if (url == null) {
                    String msg = MessageFormat.format(
                            "The assay file of type {0} / {1} for study {2} has a data file node without file name, ignoring",
                            assay.getMeasurement().getName(),
                            assay.getTechnologyName(),
                            assay.getStudy().getAcc()
                    );
                    nonRepeatedMessages.add(msg + ". Data node is " + data.getName());
                    log.trace(msg);
                    return false;
                }

                FILE.Filetype.Enum xfileType = null;
                String fileType = StringUtils.trimToNull(data.getSingleAnnotationValue("comment:SRA File Type"));
                if (fileType == null) {
                    // Let's try to get it from the file extension
                    //
                    fileType = StringUtils.trimToNull(FilenameUtils.getExtension(url));
                    if (fileType != null) {
                        xfileType = FILE.Filetype.Enum.forString(fileType.toLowerCase());
                    }

                    if (xfileType == null) {
                        String msg = MessageFormat.format(
                                "The assay file of type {0} / {1} for study {2} has a data file node without the annotation " +
                                        "'SRA file type' and I cannot compute the file type from the file name, ignoring the assay",
                                assay.getMeasurement().getName(),
                                assay.getTechnologyName(),
                                assay.getStudy().getAcc()
                        );
                        nonRepeatedMessages.add(msg);
                        log.trace(msg + ". Data node is " + data.getName());
                        return false;
                    }
                }

                if (xfileType == null) {
                    // fileType is certainly non null at this point, cause it was explicitly provided and so we
                    // have to process it
                    //
                    xfileType = FILE.Filetype.Enum.forString(fileType.toLowerCase());

                    if (xfileType == null) {
                        String msg = MessageFormat.format(
                                "The assay file of type {0} / {1} for study {2} has a bad 'SRA File Type' annotation: '" + fileType + "'" +
                                        ", ignoring the assay",
                                assay.getMeasurement().getName(),
                                assay.getTechnologyName(),
                                assay.getStudy().getAcc()
                        );
                        nonRepeatedMessages.add(msg);
                        log.trace(msg + ". Data node is " + data.getName());
                        return false;
                    }
                }


                RunType xrun = RunType.Factory.newInstance();
                xrun.setAlias(assayAcc);
                xrun.setCenterName(centerName);
                xrun.setBrokerName(brokerName);


                DATABLOCK dataBlock = DATABLOCK.Factory.newInstance();
                FILES xfiles = FILES.Factory.newInstance();
                FILE xfile = FILE.Factory.newInstance();
                xfile.setFiletype(xfileType);
                xfile.setFilename(url);

                xfile.setChecksumMethod(ChecksumMethod.MD_5);


                String md5;

                if (!fileToMD5.containsKey(url)) {

                    try {
                        md5 = IOUtils.getMD5(new File(this.sourcePath + "/" + url));
                        fileToMD5.put(url, md5);
                    } catch (NoSuchAlgorithmException e) {
                        throw new TabInternalErrorException(
                                "Problem while trying to compute the MD5 for '" + url + "': " + e.getMessage(), e
                        );
                    } catch (IOException e) {
                        throw new TabIOException(
                                "I/O problem while trying to compute the MD5 for '" + url + "': " + e.getMessage(), e
                        );

                    }
                }

                xfile.setChecksum(fileToMD5.get(url));

                xfiles.addNewFILE();
                xfiles.setFILEArray(0, xfile);
                dataBlock.setFILES(xfiles);
                xrun.addNewDATABLOCK();
                xrun.setDATABLOCKArray(xrun.sizeOfDATABLOCKArray() - 1, dataBlock);

                addExportedSubmissionFile(xsubFiles, url);
                // TODO: remove, it's deprecated now xrun.setTotalDataBlocks ( BigInteger.ONE );
                xrun.setEXPERIMENTREF(xexpRef);

                xrunSet.addNewRUN();
                xrunSet.setRUNArray(xrunSet.sizeOfRUNArray() - 1, xrun);
            }

            xexperimentSet.addNewEXPERIMENT();
            xexperimentSet.setEXPERIMENTArray(xexperimentSet.sizeOfEXPERIMENTArray() - 1, xexp);
        }

        return true;
    }


    /**
     * Builds the SRA {@link LIBRARYDESCRIPTOR}, this is taken from the ISATAB "library construction" protocol that has
     * been used for this assay.
     * <p/>
     * Some of these parameters are mandatory in SRA, and/or constrained to certain values, so the method raises an
     * exception in case they're not defined.
     */
    protected LIBRARYDESCRIPTOR buildExportedLibraryDescriptor(Assay assay) {
        ProtocolApplication papp = getProtocol(assay, "library construction");
        if (papp == null) {
            return null;
        }

        LIBRARYDESCRIPTOR xlib = LIBRARYDESCRIPTOR.Factory.newInstance();
        // xlib.setLIBRARYNAME(getParameterValue(assay, papp, "library name", true));
        // TODO check it is one of the Enum types

        xlib.setLIBRARYSTRATEGY(LIBRARYSTRATEGY.Enum.forString(
                getParameterValue(assay, papp, "library strategy", true
                )));
        xlib.setLIBRARYSOURCE(LIBRARYSOURCE.Enum.forString(
                getParameterValue(assay, papp, "library source", true
                )));
        xlib.setLIBRARYSELECTION(LIBRARYSELECTION.Enum.forString(
                getParameterValue(assay, papp, "library selection", true
                )));

        StringBuffer protocol = new StringBuffer();

        String pDescription = StringUtils.trimToNull(papp.getProtocol().getDescription());
        if (pDescription != null) {
            protocol.append("\n protocol_description: " + pDescription);
        }


        String pBibRef = getParameterValue(assay, papp, "nucl_acid_amp", false);
        if (pBibRef != null) {
            protocol.append("\n nucl_acid_amp: ").append(pBibRef);
        }

        String pUrl = getParameterValue(assay, papp, "url", false);
        if (pUrl != null) {
            protocol.append("\n url: ").append(pUrl);
        }

        String targetTaxon = getParameterValue(assay, papp, "target taxon", false);
        if (targetTaxon != null) {
            protocol.append("\n target_taxon: ").append(targetTaxon);
        }

        String targetGene = getParameterValue(assay, papp, "target_gene", false);
        if (targetGene != null) {
            protocol.append("\n target_gene: ").append(targetGene);
        }

        String targetSubfrag = getParameterValue(assay, papp, "target_subfragment", false);
        if (targetSubfrag != null) {
            protocol.append("\n target_subfragment: ").append(targetSubfrag);
        }

        String mid = getParameterValue(assay, papp, "mid", false);
        if (mid != null) {
            protocol.append("\n mid: ").append(mid);

        }


        String pcrPrimers = getParameterValue(assay, papp, "pcr_primers", false);
        if (pcrPrimers != null) {
            protocol.append("\n pcr_primers: ").append(pcrPrimers.replaceAll("=", ":"));

        }

        String pcrConditions = getParameterValue(assay, papp, "pcr_cond", false);
        if (pcrConditions != null) {
            protocol.append("\n pcr_cond: ").append(pcrConditions.replaceAll("=", ":"));

        }


        xlib.setLIBRARYCONSTRUCTIONPROTOCOL(protocol.toString());

        String libLayout = getParameterValue(assay, papp, "library layout", true);

        LIBRARYLAYOUT xlibLayout = LIBRARYLAYOUT.Factory.newInstance();
        if ("single".equalsIgnoreCase(libLayout)) {
            xlibLayout.addNewSINGLE();
            xlib.setLIBRARYLAYOUT(xlibLayout);
        } else if ("paired".equalsIgnoreCase(libLayout)) {
            // todo check with philippe about these parameters
            xlibLayout.addNewPAIRED();
            xlib.setLIBRARYLAYOUT(xlibLayout);
        } else {
            throw new TabInvalidValueException(i18n.msg(
                    "sra_invalid_param",
                    assay.getMeasurement().getName(),
                    assay.getTechnologyName(),
                    assay.getStudy().getAcc(),
                    "Library Layout/type",
                    libLayout
            ));
        }
        xlib.setLIBRARYLAYOUT(xlibLayout);

        LIBRARYDESCRIPTOR.TARGETEDLOCI xtargetedloci = LIBRARYDESCRIPTOR.TARGETEDLOCI.Factory.newInstance();

        LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS xlocus = LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS.Factory.newInstance();

        String locus = getParameterValue(assay, papp, "target_gene", false);

        if (locus != null) {
            if (locus.toLowerCase().contains("16s")) {
                xlocus.setLocusName(LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS.LocusName.X_16_S_R_RNA);
            } else {
                xlocus.setLocusName(LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS.LocusName.OTHER);
            }
        }

        LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS[] xlocusArray = new LIBRARYDESCRIPTOR.TARGETEDLOCI.LOCUS[]{xlocus};
        xtargetedloci.setLOCUSArray(xlocusArray);

        xlib.setTARGETEDLOCI(xtargetedloci);

        String pooling = getParameterValue(assay, papp, "mid", false);
        if (pooling != null) {
            LIBRARYDESCRIPTOR.POOLINGSTRATEGY xpoolingstrategy = LIBRARYDESCRIPTOR.POOLINGSTRATEGY.Factory.newInstance();
            xlib.setPOOLINGSTRATEGY(LIBRARYDESCRIPTOR.POOLINGSTRATEGY.MULTIPLEXED_LIBRARIES);

        }


        return xlib;
    }


    private Map<SequencingProperties, String> getSequencingInstrumentAndLayout(Assay assay) {
        ProtocolApplication sequencingPApp = getProtocol(assay, "DNA sequencing");

        String sequencingPlatform = getParameterValue(assay, sequencingPApp, SequencingProperties.SEQUENCING_PLATFORM.toString(), true);

        // the Library layout requirement is placed on library creation not sequencing step, hence removed
        ProtocolApplication libConstructionPApp = getProtocol(assay, "library construction");

        String sequencingLibrary = getParameterValue(assay, libConstructionPApp, SequencingProperties.LIBRARY_LAYOUT.toString(), true);

        Map<SequencingProperties, String> properties = new HashMap<SequencingProperties, String>();

        properties.put(SequencingProperties.LIBRARY_LAYOUT, sequencingLibrary);
        properties.put(SequencingProperties.SEQUENCING_PLATFORM, sequencingPlatform);

        return properties;
    }


    private SRATemplate getSRATemplateToInject(SRASection sraSection, Map<SequencingProperties, String> sequencingProperties, boolean isBarcoded) {

        return SRAUtils.getTemplate(sraSection, sequencingProperties.get(SequencingProperties.SEQUENCING_PLATFORM),
                sequencingProperties.get(SequencingProperties.LIBRARY_LAYOUT), isBarcoded);
    }

    private SRATemplate getSRATemplateToInject(SRASection sraSection, Map<SequencingProperties, String> sequencingProperties) {
        return getSRATemplateToInject(sraSection, sequencingProperties, false);
    }

    /**
     * Builds the SRA {@link SpotDescriptorType}, this is taken from the ISATAB "sequencing" protocol that has
     * been used for this assay.
     * <p/>
     * Some of these parameters are mandatory in SRA, and/or constrained to certain values, so the method raises an
     * exception in case they're not defined.
     */
    protected SpotDescriptorType buildExportedSpotDescriptor(Assay assay, Map<SequencingProperties, String> sequencingProperties) {

        String[] barcodes = getBarcodesForAssays(assay);

        ProtocolApplication pApp = getProtocol(assay, "DNA sequencing");
        if (pApp == null) {
            return null;
        }

        String adapterSpec = getParameterValue(assay, pApp, "Adapter Spec", false);
        String numOfSpotReads = getParameterValue(assay, pApp, "Number of reads per spot", false);

        boolean usesBarcode = (barcodes.length > 0);

        SRATemplate sraTemplateToInject = getSRATemplateToInject(SRASection.SPOT_DESCRIPTOR, sequencingProperties, usesBarcode);

        SpotDescriptorType xspotd = SpotDescriptorType.Factory.newInstance();

        Map<SRAAttributes, String> userDefinedAttributes = new HashMap<SRAAttributes, String>();

        if (!StringUtils.isEmpty(adapterSpec)) {
            userDefinedAttributes.put(SRAAttributes.ADAPTER_SPEC, adapterSpec);
        }

        if (!StringUtils.isEmpty(numOfSpotReads)) {
            userDefinedAttributes.put(SRAAttributes.NUMBER_OF_READS_PER_SPOT, numOfSpotReads);
        }

        if (barcodes.length > 0 && !StringUtils.isEmpty(barcodes[0])) {
            userDefinedAttributes.put(SRAAttributes.READ_GROUP_TAG, barcodes[0]);
        }

        try {
            String sraTemplate = sraTemplateLoader.getSRAProcessingTemplate(sraTemplateToInject, userDefinedAttributes);

            XmlOptions xmlOptions = new XmlOptions();
            xmlOptions.setDocumentType(SpotDescriptorType.Factory.newInstance().schemaType());

            XmlObject parsedAttr =
                    XmlObject.Factory.parse(sraTemplate, xmlOptions);

            xspotd.set(parsedAttr);

            // now output rest of the barcodes (if there is more than one :) )

            if (barcodes.length > 1) {
                // output the barcode

                SpotDescriptorType.SPOTDECODESPEC.READSPEC readSpec = getReadSpecWithBaseCalls(xspotd.getSPOTDECODESPEC());

                // create new array of base calls and place the already added base call item into the array

                SpotDescriptorType.SPOTDECODESPEC.READSPEC.EXPECTEDBASECALLTABLE.BASECALL[] baseCallArray = new SpotDescriptorType.SPOTDECODESPEC.READSPEC.EXPECTEDBASECALLTABLE.BASECALL[barcodes.length];

                SpotDescriptorType.SPOTDECODESPEC.READSPEC.EXPECTEDBASECALLTABLE table = readSpec.getEXPECTEDBASECALLTABLE();


                for (int barcodeIndex = 0; barcodeIndex < barcodes.length; barcodeIndex++) {

                    SpotDescriptorType.SPOTDECODESPEC.READSPEC.EXPECTEDBASECALLTABLE.BASECALL baseCall = SpotDescriptorType.SPOTDECODESPEC.READSPEC.EXPECTEDBASECALLTABLE.BASECALL.Factory.newInstance();
                    baseCall.setReadGroupTag(barcodes[barcodeIndex]);
                    baseCall.setStringValue(barcodes[barcodeIndex]);

                    baseCallArray[barcodeIndex] = baseCall;
                }

                System.out.println("BASE CALL TABLE: " + readSpec.getEXPECTEDBASECALLTABLE());
                System.out.println("MY BASE CALL ARRAY: \n"  + baseCallArray[0].toString());
                readSpec.getEXPECTEDBASECALLTABLE().setBASECALLArray(baseCallArray);
            }


            return xspotd;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlException e) {
            e.printStackTrace();
        }

        return xspotd;
    }



    private SpotDescriptorType.SPOTDECODESPEC.READSPEC getReadSpecWithBaseCalls(SpotDescriptorType.SPOTDECODESPEC spotDecodeSpec) {
        if(spotDecodeSpec.getREADSPECArray().length > 0) {
            for(SpotDescriptorType.SPOTDECODESPEC.READSPEC readSpec : spotDecodeSpec.getREADSPECArray()) {
                if(readSpec.getEXPECTEDBASECALLTABLE() != null) {
                    return readSpec;
                }
            }
        }

        return null;
    }

    private String[] getBarcodesForAssays(Assay assay) {

        ProtocolApplication libConstructionPApp = getProtocol(assay, "library construction");
        if (libConstructionPApp == null) {
            return null;
        }

        String[] barcodes = getParameterValues(assay, libConstructionPApp, "mid", false);

        return (barcodes == null) ? new String[0] : barcodes;
    }


    /**
     * Builds the SRA {@link PROCESSING}, this is taken from the ISATAB "sequencing" protocol that has
     * been used for this assay.
     * <p/>
     * Some of these parameters are mandatory in SRA, and/or constrained to certain values, so the method raises an
     * exception in case they're not defined.
     */
    protected PROCESSING buildExportedProcessing(final Assay assay, Map<SequencingProperties, String> sequencingProperties) {
        ProtocolApplication pApp = getProtocol(assay, "DNA sequencing");

        SRATemplate sraTemplateToInject = getSRATemplateToInject(SRASection.PROCESSING, sequencingProperties);

        String seqSpaceStr = getParameterValue(assay, pApp, "Sequence space", false);

        String baseCaller = getParameterValue(assay, pApp, "Base caller", false);

        String qualityScorer = getParameterValue(assay, pApp, "Quality scorer", false);

        String numberOfLevels = getParameterValue(assay, pApp, "Number of levels", false);

        String multiplier = getParameterValue(assay, pApp, "Multiplier", false);

        Map<SRAAttributes, String> userDefinedAttributes = new HashMap<SRAAttributes, String>();

        if (!StringUtils.isEmpty(seqSpaceStr)) {
            userDefinedAttributes.put(SRAAttributes.SEQUENCE_SPACE, seqSpaceStr);
        }

        if (!StringUtils.isEmpty(baseCaller)) {
            userDefinedAttributes.put(SRAAttributes.BASE_CALLER, baseCaller);
        }

        if (!StringUtils.isEmpty(qualityScorer)) {
            userDefinedAttributes.put(SRAAttributes.QUALITY_SCORER, qualityScorer);
        }

        if (!StringUtils.isEmpty(numberOfLevels)) {
            userDefinedAttributes.put(SRAAttributes.NUMBER_OF_LEVELS, numberOfLevels);
        }

        if (!StringUtils.isEmpty(multiplier)) {
            userDefinedAttributes.put(SRAAttributes.MULTIPLIER, multiplier);
        }


        // TODO: modify to pull out the technology and library using Parameter Value[platform] & ParameterValue[library layout] respectively
        // TODO: PRS-> according to new configuration for sequencing, Parameter Value[library layout] is moved to the library construction protocol
        // TODO: PRS-> replace Parameter Value[platform] with Parameter Value[sequencing instrument] and checks on values.
        // TODO: PRS-> add support for Immunoprecipitation techniques, requires detection of Protocol
        // TODO: PRS-> add support for 'Targeted Loci' in SRA experiment and find in ISA-TAB file if assay is environmental gene survey


        try {
            String sraTemplate = sraTemplateLoader.getSRAProcessingTemplate(sraTemplateToInject, userDefinedAttributes);

            XmlOptions xmlOptions = new XmlOptions();
            xmlOptions.setDocumentType(PROCESSING.Factory.newInstance().schemaType());

            PROCESSING processing = PROCESSING.Factory.newInstance();

            XmlObject processingObject = XmlObject.Factory.parse(sraTemplate, xmlOptions);

            processing.set(processingObject);
            return processing;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlException e) {
            e.printStackTrace();
        }

        return PROCESSING.Factory.newInstance();

    }


    /**
     * Builds the SRA {@link PlatformType}, this is partly taken from the ISATAB "sequencing" protocol and partly from the
     * "platform" field in the ISATAB assay section (investigation file).
     * <p/>
     * Some of these parameters are mandatory in SRA, and/or constrained to certain values, so the method raises an
     * exception in case they're not defined.
     * TODO: this could be replaced by relying on the ISA Parameter Value[sequencing instrument] available from latest configuration
     */
    protected PlatformType buildExportedPlatform(final Assay assay) {
        ProtocolApplication pApp = getProtocol(assay, "DNA sequencing");
        if (pApp == null) {
            return null;
        }
        Protocol proto = pApp.getProtocol();

        // Get the instrument information associated to that sequencing protocol
        // TODO: PRS: rely on a ISA Parameter Value[sequencing instrument] instead to obtain the information
        // TODO: PRS: check against the declared ISA assay platform


        String sequencinginst = getParameterValue(assay, pApp, "sequencing instrument", true);

        PlatformType.LS454.INSTRUMENTMODEL.Enum.forString(sequencinginst);

        if (("454 GS".equalsIgnoreCase(sequencinginst) ||
                "454 GS 20".equalsIgnoreCase(sequencinginst) ||
                "454 GS FLX".equalsIgnoreCase(sequencinginst) ||
                "454 GS FLX Titanium".equalsIgnoreCase(sequencinginst) ||
                "454 GS Junior".equalsIgnoreCase(sequencinginst) ||
                "GS20".equalsIgnoreCase(sequencinginst) ||
                "GS FLX".equalsIgnoreCase(sequencinginst)
        )) {

            // todo finish
        }


        String xinstrument = null;

        for (ProtocolComponent pcomp : proto.getComponents()) {
            for (OntologyTerm ctype : pcomp.getOntologyTerms()) {
                String pctypeStr = ctype.getName().toLowerCase();
                if (pctypeStr.contains("instrument") || pctypeStr.contains("sequencer")) {
                    xinstrument = pcomp.getValue();
                    break;
                }
            }
        }

        if (xinstrument == null) {
            String msg = MessageFormat.format(
                    "The assay file of type {0} / {1} for study {2} has no Instrument declared in the ISA Sequencing Protocol",
                    assay.getMeasurement().getName(),
                    assay.getTechnologyName(),
                    assay.getStudy().getAcc()
            );
            throw new TabMissingValueException(msg);
        }


        PlatformType xplatform = PlatformType.Factory.newInstance();
        String platform = StringUtils.upperCase(assay.getAssayPlatform());


        if (platform.toLowerCase().contains("454")) {


            //if ("LS454".equalsIgnoreCase(platform)) {

            PlatformType.LS454 ls454 = PlatformType.LS454.Factory.newInstance();
            ls454.setINSTRUMENTMODEL(PlatformType.LS454.INSTRUMENTMODEL.Enum.forString(xinstrument));

            //String keyseqStr = "TACG";


            //ls454.setKEYSEQUENCE(keyseqStr);

            String flowSeqstr = "TACG";

            ls454.setFLOWSEQUENCE(flowSeqstr);

            int flowCount = 800;

            //String flowCountStr = getParameterValue(assay, pApp, "Flow Count", false);
            //ls454.setFLOWCOUNT(new BigInteger(checkNumericParameter(flowCountStr)));
            ls454.setFLOWCOUNT(BigInteger.valueOf(flowCount));
            xplatform.setLS454(ls454);

        } else if (platform.toLowerCase().contains("illumina")) {
            PlatformType.ILLUMINA illumina = PlatformType.ILLUMINA.Factory.newInstance();
            illumina.setINSTRUMENTMODEL(PlatformType.ILLUMINA.INSTRUMENTMODEL.Enum.forString(xinstrument));
            illumina.setCYCLESEQUENCE(getParameterValue(assay, pApp, "Cycle Sequence", true));
            illumina.setCYCLECOUNT(new BigInteger(checkNumericParameter(getParameterValue(assay, pApp, "Cycle Count", true))));
            xplatform.setILLUMINA(illumina);

        } else if (platform.toLowerCase().contains("helicos")) {
            //("HELICOS".equalsIgnoreCase(platform)) {
            PlatformType.HELICOS helicos = PlatformType.HELICOS.Factory.newInstance();
            helicos.setINSTRUMENTMODEL(PlatformType.HELICOS.INSTRUMENTMODEL.Enum.forString(xinstrument));
            helicos.setFLOWSEQUENCE(getParameterValue(assay, pApp, "Flow Sequence", true));
            helicos.setFLOWCOUNT(new BigInteger(checkNumericParameter(getParameterValue(assay, pApp, "Flow Count", true))));
            xplatform.setHELICOS(helicos);

        } else if (platform.toLowerCase().contains("solid")) {
            // ("ABI SOLID".equalsIgnoreCase(platform) || "ABI_SOLID".equalsIgnoreCase(platform)) {
            PlatformType.ABISOLID abisolid = PlatformType.ABISOLID.Factory.newInstance();
            abisolid.setINSTRUMENTMODEL(PlatformType.ABISOLID.INSTRUMENTMODEL.Enum.forString(xinstrument));

            {
                String colorMatrix = getParameterValue(assay, pApp, "Color Matrix", false);
                // single dibase colours are semicolon-separated
                if (colorMatrix != null) {

                    PlatformType.ABISOLID.COLORMATRIX xcolorMatrix = PlatformType.ABISOLID.COLORMATRIX.Factory.newInstance();

                    String dibases[] = colorMatrix.split("\\;");
                    if (dibases != null && dibases.length > 0) {

                        PlatformType.ABISOLID.COLORMATRIX.COLOR xcolors[] = new PlatformType.ABISOLID.COLORMATRIX.COLOR[dibases.length];
                        int i = 0;
                        for (String dibase : dibases) {
                            PlatformType.ABISOLID.COLORMATRIX.COLOR xcolor = PlatformType.ABISOLID.COLORMATRIX.COLOR.Factory.newInstance();
                            xcolor.setDibase(dibase);
                            xcolors[i++] = xcolor;
                        }
                        xcolorMatrix.setCOLORArray(xcolors);
                        abisolid.setCOLORMATRIX(xcolorMatrix);
                    }
                }
            }

            {
                String colorMatrixCode = getParameterValue(assay, pApp, "Color Matrix Code", false);
                if (colorMatrixCode != null) {
                    abisolid.setCOLORMATRIXCODE(colorMatrixCode);
                }
            }

            // TODO: remove, deprecated abisolid.setCYCLECOUNT ( new BigInteger ( getParameterValue ( assay, papp, "Cycle Count", true ) ) );

            abisolid.setSEQUENCELENGTH(new BigInteger(checkNumericParameter(getParameterValue(assay, pApp, "Cycle Count", false))));

            xplatform.setABISOLID(abisolid);
        } else {
            throw new TabInvalidValueException(MessageFormat.format(
                    "The SRA platform ''{0}'' for the assay ''{1}''/''{2}'' in the study ''{3}'' is invalid. Please supply the Platform information for the Assay in the Investigation file",
                    platform, assay.getMeasurement().getName(), assay.getTechnologyName(), assay.getStudy().getAcc()
            ));
        }

        return xplatform;
    }

    private String checkNumericParameter(String parameterValue) {

        return parameterValue == null || parameterValue.trim().equals("") ? "0" : parameterValue;

    }
}