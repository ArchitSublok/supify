package com.supify.util;

import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationTask;
import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.EvidenceKind;
import com.supify.model.enums.ListingState;
import com.supify.model.enums.Pillar;
import com.supify.model.enums.TrustBand;
import com.supify.model.enums.VerificationMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Populates canonical supplier fixtures, claims, and a 35-second expiring attestation. */
public class SeedDataFactory {

    public static void populate(SupplierRepository supplierRepo, VerificationRepository verificationRepo) {
        // Supplier 1: Aravind Fasteners Private Limited (Coimbatore, TN)
        Organisation aravindOrg = new Organisation(
                "Aravind Fasteners Private Limited",
                "Aravind Fasteners",
                "pvt_ltd",
                "IN",
                "Coimbatore",
                "Tamil Nadu",
                2008
        );
        supplierRepo.saveOrganisation(aravindOrg);

        SupplierProfile aravindProfile = new SupplierProfile(
                aravindOrg.getId(),
                "Precision cold-forged fasteners, automotive bolts and aerospace-grade hex screws",
                "Fasteners & Cold Heading",
                50000,
                "kg",
                1000,
                "14 days"
        );
        aravindProfile.setTrustBand(TrustBand.VERIFIED);
        aravindProfile.setOverallTrustScore(0.840);
        supplierRepo.saveProfile(aravindProfile);

        Map<Pillar, Double> aravindPillars = new EnumMap<>(Pillar.class);
        aravindPillars.put(Pillar.IDENTITY, 0.92);
        aravindPillars.put(Pillar.LEGAL, 0.86);
        aravindPillars.put(Pillar.QUALITY, 0.68);
        aravindPillars.put(Pillar.CAPABILITY, 0.58);
        supplierRepo.saveSnapshot(new TrustSnapshot(aravindOrg.getId(), TrustBand.VERIFIED, 0.840, aravindPillars));

        // Claim 1: GSTIN
        Claim gstinClaim = new Claim(aravindOrg.getId(), "gst_registration", "GSTIN Active Registration", Pillar.IDENTITY, "33AABCA1234F1Z8");
        gstinClaim.submit();
        verificationRepo.saveClaim(gstinClaim);

        Evidence gstinEvidence = new Evidence(gstinClaim.getId(), "ref:gst_cert_2026.pdf", EvidenceKind.PDF, "GST Registration Certificate", Instant.now().minus(Duration.ofDays(90)), null);
        verificationRepo.saveEvidence(gstinEvidence);

        VerificationCheck gstinCheck = new VerificationCheck(gstinClaim.getId(), VerificationMethod.THIRD_PARTY_API, CheckOutcome.PASS, 1.0, "gst_adapter@v1", "Registry verified ACTIVE on GST portal.");
        verificationRepo.recordCheck(gstinCheck);

        Attestation gstinAttestation = new Attestation(gstinClaim.getId(), gstinCheck.getId(), VerificationMethod.THIRD_PARTY_API, Instant.now().plus(Duration.ofDays(180)), "SYSTEM_ADAPTER");
        verificationRepo.saveAttestation(gstinAttestation);

        // Claim 2: ISO 9001
        Claim isoClaim = new Claim(aravindOrg.getId(), "iso_9001", "ISO 9001:2015 Quality Management", Pillar.QUALITY, "ISO-9001:2015 - Reg #QA-98421");
        isoClaim.submit();
        verificationRepo.saveClaim(isoClaim);

        Evidence isoEvidence = new Evidence(isoClaim.getId(), "ref:tuv_iso9001.pdf", EvidenceKind.PDF, "TUV Rheinland Certificate", Instant.now().minus(Duration.ofDays(200)), Instant.now().plus(Duration.ofDays(365)));
        verificationRepo.saveEvidence(isoEvidence);

        VerificationCheck isoCheck = new VerificationCheck(isoClaim.getId(), VerificationMethod.DOCUMENT_REVIEW, CheckOutcome.PASS, 0.95, "senior_auditor_01", "Accredited certificate matches Registrar QR code.");
        verificationRepo.recordCheck(isoCheck);

        Attestation isoAttestation = new Attestation(isoClaim.getId(), isoCheck.getId(), VerificationMethod.DOCUMENT_REVIEW, Instant.now().plus(Duration.ofDays(165)), "senior_auditor_01");
        verificationRepo.saveAttestation(isoAttestation);

        // Claim 3: DEMO TARGET - 35-Second Expiring Attestation for Live Decay Demonstration!
        Claim capacityClaim = new Claim(aravindOrg.getId(), "annual_capacity", "Cold Heading Production Capacity (50T/mo)", Pillar.CAPABILITY, "50,000 kg / month rated capacity");
        capacityClaim.submit();
        verificationRepo.saveClaim(capacityClaim);

        VerificationCheck capCheck = new VerificationCheck(capacityClaim.getId(), VerificationMethod.SITE_VISIT, CheckOutcome.PASS, 1.0, "auditor_chennai", "On-site machine audit verified 4 high-speed forging headers.");
        verificationRepo.recordCheck(capCheck);

        // Expires in 35 seconds!
        Attestation expiringAttestation = new Attestation(capacityClaim.getId(), capCheck.getId(), VerificationMethod.SITE_VISIT, Instant.now().plusSeconds(35), "auditor_chennai");
        verificationRepo.saveAttestation(expiringAttestation);

        // Queue Task 1 (SLA Target: within 1 hour so SLA escalation daemon escalates it!)
        VerificationTask task1 = new VerificationTask(capacityClaim.getId(), aravindOrg.getId(), "Aravind Fasteners", "Cold Heading Production Capacity (50T/mo)", 95, false, Instant.now().plus(Duration.ofHours(1)));
        verificationRepo.enqueueTask(task1);

        // Supplier 2: Monarch Industrial Assemblies (Pune, Maharashtra)
        Organisation monarchOrg = new Organisation(
                "Monarch Industrial Assemblies",
                "Monarch Industries",
                "pvt_ltd",
                "IN",
                "Pune",
                "Maharashtra",
                2012
        );
        supplierRepo.saveOrganisation(monarchOrg);

        SupplierProfile monarchProfile = new SupplierProfile(
                monarchOrg.getId(),
                "Custom precision sheet metal fabrication, laser cutting, and robotic CNC bending",
                "Custom Sheet Metal",
                12000,
                "units",
                200,
                "21 days"
        );
        monarchProfile.setTrustBand(TrustBand.AUDITED);
        monarchProfile.setOverallTrustScore(0.890);
        supplierRepo.saveProfile(monarchProfile);

        Map<Pillar, Double> monarchPillars = new EnumMap<>(Pillar.class);
        monarchPillars.put(Pillar.IDENTITY, 0.95);
        monarchPillars.put(Pillar.LEGAL, 0.90);
        monarchPillars.put(Pillar.QUALITY, 0.92);
        monarchPillars.put(Pillar.CAPABILITY, 0.81);
        supplierRepo.saveSnapshot(new TrustSnapshot(monarchOrg.getId(), TrustBand.AUDITED, 0.890, monarchPillars));

        Claim monarchClaim = new Claim(monarchOrg.getId(), "iso_14001", "ISO 14001 Environmental Management", Pillar.COMPLIANCE, "ISO-14001:2015 Environmental System");
        monarchClaim.submit();
        verificationRepo.saveClaim(monarchClaim);

        VerificationTask task2 = new VerificationTask(monarchClaim.getId(), monarchOrg.getId(), "Monarch Industrial Assemblies", "ISO 14001 Environmental Management", 90, true, Instant.now().plus(Duration.ofHours(4)));
        verificationRepo.enqueueTask(task2);

        // Supplier 3: Kavach Industrial Safety Systems (Ahmedabad, Gujarat)
        Organisation kavachOrg = new Organisation(
                "Kavach Industrial Safety Systems",
                "Kavach Safety",
                "pvt_ltd",
                "IN",
                "Ahmedabad",
                "Gujarat",
                2016
        );
        supplierRepo.saveOrganisation(kavachOrg);

        SupplierProfile kavachProfile = new SupplierProfile(
                kavachOrg.getId(),
                "Certified industrial safety harnesses, fall arresters, and PPE systems for heavy industry",
                "Industrial Safety Equipment",
                25000,
                "units",
                500,
                "10 days"
        );
        kavachProfile.setTrustBand(TrustBand.VERIFIED);
        kavachProfile.setOverallTrustScore(0.760);
        supplierRepo.saveProfile(kavachProfile);

        Map<Pillar, Double> kavachPillars = new EnumMap<>(Pillar.class);
        kavachPillars.put(Pillar.IDENTITY, 0.90);
        kavachPillars.put(Pillar.LEGAL, 0.82);
        kavachPillars.put(Pillar.QUALITY, 0.70);
        kavachPillars.put(Pillar.CAPABILITY, 0.62);
        supplierRepo.saveSnapshot(new TrustSnapshot(kavachOrg.getId(), TrustBand.VERIFIED, 0.760, kavachPillars));

        Claim kavachClaim = new Claim(kavachOrg.getId(), "pollution_control", "State PCB Consent to Operate (CTO)", Pillar.COMPLIANCE, "Gujarat PCB Consent Order #GPCB/CTO/2024-29");
        kavachClaim.submit();
        verificationRepo.saveClaim(kavachClaim);

        VerificationTask task3 = new VerificationTask(kavachClaim.getId(), kavachOrg.getId(), "Kavach Industrial Safety Systems", "State PCB Consent to Operate (CTO)", 75, false, Instant.now().plus(Duration.ofHours(18)));
        verificationRepo.enqueueTask(task3);

        // Supplier 4: Zenith Aerospace Electronics (Bengaluru, Karnataka)
        Organisation zenithOrg = new Organisation(
                "Zenith Aerospace Electronics",
                "Zenith Aero",
                "public_ltd",
                "IN",
                "Bengaluru",
                "Karnataka",
                2004
        );
        supplierRepo.saveOrganisation(zenithOrg);

        SupplierProfile zenithProfile = new SupplierProfile(
                zenithOrg.getId(),
                "MIL-spec avionics wiring harnesses, high-density multilayer PCBs and flight controller assemblies",
                "Aerospace Harnesses & PCBs",
                5000,
                "boards",
                50,
                "30 days"
        );
        zenithProfile.setTrustBand(TrustBand.AUDITED);
        zenithProfile.setOverallTrustScore(0.940);
        supplierRepo.saveProfile(zenithProfile);

        Map<Pillar, Double> zenithPillars = new EnumMap<>(Pillar.class);
        zenithPillars.put(Pillar.IDENTITY, 0.98);
        zenithPillars.put(Pillar.LEGAL, 0.94);
        zenithPillars.put(Pillar.QUALITY, 0.96);
        zenithPillars.put(Pillar.CAPABILITY, 0.88);
        supplierRepo.saveSnapshot(new TrustSnapshot(zenithOrg.getId(), TrustBand.AUDITED, 0.940, zenithPillars));

        // Supplier 5: Suryodaya Solar Tech LLP (Jaipur, Rajasthan)
        Organisation suryodayaOrg = new Organisation(
                "Suryodaya Solar Tech LLP",
                "Suryodaya Solar",
                "llp",
                "IN",
                "Jaipur",
                "Rajasthan",
                2019
        );
        supplierRepo.saveOrganisation(suryodayaOrg);

        SupplierProfile suryodayaProfile = new SupplierProfile(
                suryodayaOrg.getId(),
                "Galvanized solar module mounting structures, ground-mount tracker pylons and rooftop rails",
                "Solar Mounting Structures",
                80000,
                "brackets",
                2500,
                "18 days"
        );
        suryodayaProfile.setTrustBand(TrustBand.BASIC);
        suryodayaProfile.setOverallTrustScore(0.650);
        supplierRepo.saveProfile(suryodayaProfile);

        Map<Pillar, Double> suryodayaPillars = new EnumMap<>(Pillar.class);
        suryodayaPillars.put(Pillar.IDENTITY, 0.85);
        suryodayaPillars.put(Pillar.LEGAL, 0.70);
        suryodayaPillars.put(Pillar.QUALITY, 0.55);
        suryodayaPillars.put(Pillar.CAPABILITY, 0.60);
        supplierRepo.saveSnapshot(new TrustSnapshot(suryodayaOrg.getId(), TrustBand.BASIC, 0.650, suryodayaPillars));
    }
}
