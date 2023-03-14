package raf.bolnica1.patient.services.impl;


import org.springframework.stereotype.Service;

import raf.bolnica1.patient.domain.GeneralMedicalData;
import raf.bolnica1.patient.domain.MedicalRecord;
import raf.bolnica1.patient.domain.Patient;
import raf.bolnica1.patient.dto.*;

import raf.bolnica1.patient.domain.*;

import raf.bolnica1.patient.mapper.*;
import raf.bolnica1.patient.repository.*;
import raf.bolnica1.patient.services.PatientService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class PatientServiceImpl implements PatientService {


    private PatientRepository patientRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private MedicalHistoryRepository medicalHistoryRepository;
    private ExaminationHistoryRepository examinationHistoryRepository;
    private GeneralMedicalDataRepository generalMedicalDataRepository;
    private SocialDataRepository socialDataRepository;
    private VaccinationDataRepository vaccinationDataRepository;
    private AllergyDataRepository allergyDataRepository;
    private OperationRepository operationRepository;


    private GeneralMedicalDataMapper generalMedicalDataMapper;
    private OperationMapper operationMapper;
    private MedicalHistoryMapper medicalHistoryMapper;


    public PatientServiceImpl(PatientRepository patientRepository, MedicalRecordRepository medicalRecordRepository,
                              GeneralMedicalDataRepository generalMedicalDataRepository, SocialDataRepository socialDataRepository,
                              MedicalHistoryRepository medicalHistoryRepository, ExaminationHistoryRepository examinationHistoryRepository,
                              VaccinationDataRepository vaccinationDataRepository,AllergyDataRepository allergyDataRepository,
                              GeneralMedicalDataMapper generalMedicalDataMapper, OperationRepository operationRepository,
                              OperationMapper operationMapper, MedicalHistoryMapper medicalHistoryMapper
    ) {
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.generalMedicalDataRepository = generalMedicalDataRepository;
        this.socialDataRepository = socialDataRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.examinationHistoryRepository = examinationHistoryRepository;
        this.vaccinationDataRepository=vaccinationDataRepository;
        this.allergyDataRepository=allergyDataRepository;
        this.generalMedicalDataMapper=generalMedicalDataMapper;
        this.operationRepository=operationRepository;
        this.operationMapper=operationMapper;
        this.medicalHistoryMapper=medicalHistoryMapper;
    }

    //Registracija pacijenta
    public PatientDto registerPatient(PatientDto dto){
        Patient patient = PatientMapper.patientDtoToPatient(dto);
        patient.setLbp(UUID.randomUUID().toString());

        patient.setSocialData(socialDataRepository.save(patient.getSocialData()));

        patient = patientRepository.save(patient);

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatient(patient);
        medicalRecord.setRegistrationDate(Date.valueOf(LocalDate.now()));

        GeneralMedicalData generalMedicalData = new GeneralMedicalData();
        generalMedicalData.setBloodType("A");
        generalMedicalData.setRH("+");
        generalMedicalData = generalMedicalDataRepository.save(generalMedicalData);

        medicalRecord.setGeneralMedicalData(generalMedicalData);
        medicalRecordRepository.save(medicalRecord);

        return dto;
    }


    //Azuriranje podataka pacijenta
    public PatientDto updatePatient(PatientDto dto){
        Optional<Patient> patient = patientRepository.findById(dto.getId());
        if(patient.isPresent()){
            PatientMapper.compareAndSet(dto, patient.get());
            socialDataRepository.save(patient.get().getSocialData());
            patientRepository.save(patient.get());
            return dto;
        }
        return null;
    }


    //Brisanje pacijenta
    public boolean deletePatient(String lbp){
        Optional<Patient> patient = patientRepository.findByLbp(lbp);
        if(patient.isPresent()){
            patient.get().setDeleted(true);
            patientRepository.save(patient.get());
        }
        else
             return false;

        Optional<MedicalRecord> medicalRecord = medicalRecordRepository.findByPatient_Lbp(lbp);
        if(medicalRecord.isPresent()){
            medicalRecord.get().setDeleted(true);
            medicalRecordRepository.save(medicalRecord.get());
            return true;
        }
        return false;
    }



    public List<PatientDto> filterPatients(String lbp, String jmbg, String name, String surname){
        List<Patient> patients = new ArrayList<>();
        Optional<List<Patient>> tempList;
        Optional<Patient> tempPatientLbp;
        Optional<Patient> tempPatientJmbg;

        if(!name.equals("")){
            tempList = patientRepository.findByName(name);
            if(tempList.isPresent())
                patients = tempList.get();
        }
        if(!surname.equals("")){
            if(patients.isEmpty()) {
                tempList = patientRepository.findBySurname(surname);
                if(tempList.isPresent())
                    patients = tempList.get();
            }
            else
                patients.removeIf(p -> !p.getSurname().equals(surname));
        }
        if(!lbp.equals("")){
            tempPatientLbp = patientRepository.findByLbp(lbp);
            if(tempPatientLbp.isPresent()) {
                if (patients.isEmpty())
                    patients.add(tempPatientLbp.get());
                else
                    patients.removeIf(p -> !p.getLbp().equals(tempPatientLbp.get().getLbp()));
            }
        }
        if(!jmbg.equals("")){
            tempPatientJmbg = patientRepository.findByJmbg(jmbg);
            if(tempPatientJmbg.isPresent()){
                if(patients.isEmpty())
                    patients.add(tempPatientJmbg.get());
                else
                    patients.removeIf(p -> !p.getLbp().equals(tempPatientJmbg.get().getLbp()));
            }
        }
        //izbacujemo sve pacijente koji su "obrisani"
        patients.removeIf(p -> p.isDeleted());

        return PatientMapper.allToDto(patients);
    }

    //Pretraga pacijenta
    public Object findPatient(Object object){
        return null;
    }


    //Pretraga pacijenta preko LBP-a
    public Patient findPatientLBP(String lbp){

        Optional<Patient> patient;
        patient = patientRepository.findByLbp(lbp);

        //Provera da li pacijent postoji, ako postoji vraca ga ako ne onda vraca null
        if(patient.isPresent()) {
            return patient.get();
        }

        return null;
    }


    ///TODO: pretraga medicalHystory nije dobra, DiagnosisCode_Id(id polje generisano u bazi) nije isto sto i mkb10 vrv
    //Dobijanje istorije bolesti pacijenta
    public List<PatientDtoDesease> hisotryOfDeseasePatient(String  lbp, Long mkb10){
        //Dohvatanje konkretnog pacijenta preko lbp-a
        Optional<Patient> patient;
        patient = patientRepository.findByLbp(lbp);

        //Dohvatanje kartona konkretnog pacijenta
        Optional<MedicalRecord> medical;
        medical =  medicalRecordRepository.findByPatient_Lbp(patient.get().getLbp());

        //Dohvatanje bolesti preko karotna i preko mkb10 (dijagnoza)
        Optional<List<MedicalHistory>> history;
        history = medicalHistoryRepository.findByMedicalRecord_IdAndDiagnosisCode_Id(medical.get().getId(), Long.valueOf(mkb10));

        //Provera da li postoji bolest ako postoji onda mapiramo na dto koji vracamo na front, ako ne postoji onda vracamo null
        if(history.isPresent()){
            return PatientDeseaseMapper.allToDto(history.get());
        }

        return null;
    }



    //Svi izvestaji
    //Dohvatanje izvestaja pregleda preko lbp-a pacijenta i preko konkretnog datuma
    public List<PatientDtoReport>  findReportPatientByCurrDate(String lbp, Date currDate){
        //Dohvatanje konkretnog pacijenta preko lbp-a
        Optional<Patient> patient;
        patient = patientRepository.findByLbp(lbp);

        //Dohvatanje kartona tog pacijenta
        Optional<MedicalRecord> medical;
        medical =  medicalRecordRepository.findByPatient_Lbp(patient.get().getLbp());

        //Dohvatanje izvestaja pregleda preko kartona konkretnog pacijenta i preko konkretnog datuma
        Optional<List<ExaminationHistory>> examination;
        examination = examinationHistoryRepository.findByMedicalRecord_IdAndExamDateEquals(medical.get().getId(),currDate);

        //Provera da li postoje izvestaji pregleda, ako postoje mapiramo na dto koji vracamo na front ako ne postoji onda vracamo null
        if(examination.isPresent()){
            return ExaminationHistoryMapper.allToDto(examination.get());
        }


        return null;
    }


    //Dohvatanje izvestaja pregleda preko lbp-a pacijenta i preko raspona datuma od-do
    public List<PatientDtoReport> findReportPatientByFromAndToDate(String lbp,Date fromDate,Date toDate){
        //Dohvatanje konkretnog pacijenta preko lbp-a
        Optional<Patient> patient;
        patient = patientRepository.findByLbp(lbp);

        //Dohvatanje kartona tog pacijenta
        Optional<MedicalRecord> medical;
        medical =  medicalRecordRepository.findByPatient_Lbp(patient.get().getLbp());

        //Dohvatanje izvestaja pregleda preko kartona konkretnog pacijenta i preko raspona datuma od-do
        Optional<List<ExaminationHistory>> examination;
        examination = examinationHistoryRepository.findByMedicalRecord_IdAndExamDateGreaterThanAndExamDateLessThan(medical.get().getId(),fromDate,toDate);

        //Provera da li postoje izvestaji pregleda, ako postoje mapiramo na dto koji vracamo na front ako ne postoji onda vracamo null
        if(examination.isPresent()){
            return ExaminationHistoryMapper.allToDto(examination.get());
        }

        return null;
    }



    //Svi kartoni
    //m22
    public MedicalRecordDto findMedicalRecordByLbp(String lbp) {

        Optional<MedicalRecord> list = medicalRecordRepository.findByPatient_Lbp(lbp);

        if(list.isPresent()){
            return MedicalRecordMapper.toDto(list.get());
        } else {
            return null;
        }
    }





    //Dohvatanje GeneralMedicalData po LBP(GMD,vaccines,allergies)
    public GeneralMedicalDataDto findGeneralMedicalDataByLbp(String lbp) {

        MedicalRecord medicalRecord=medicalRecordRepository.findByPatient_Lbp(lbp).get();
        if(medicalRecord==null)return null;
        GeneralMedicalData generalMedicalData=medicalRecord.getGeneralMedicalData();
        if(generalMedicalData==null)return null;

        List<Object[]> vaccinationsAndDates=vaccinationDataRepository.findVaccinationsByGeneralMedicalData(generalMedicalData);
        List<Allergy> allergies=allergyDataRepository.findAllergiesByGeneralMedicalData(generalMedicalData);

        GeneralMedicalDataDto dto=generalMedicalDataMapper.toDto(generalMedicalData,vaccinationsAndDates,allergies);

        return dto;
    }

    ///Dohvatanje liste operacije koje odgovaraju LBP
    public List<OperationDto> findOperationsByLbp(String lbp) {

        Optional<MedicalRecord> medicalRecord=medicalRecordRepository.findByPatient_Lbp(lbp);
        if(!medicalRecord.isPresent())return null;

        List<Operation> operations=operationRepository.findOperationsByMedicalRecord(medicalRecord.get());

        return operationMapper.toDto(operations);
    }

    ///Dohvatanje liste MedicalHistory po LBP
    public List<MedicalHistoryDto> findMedicalHystoryByLbp(String lbp) {

        Optional<MedicalRecord> medicalRecord=medicalRecordRepository.findByPatient_Lbp(lbp);
        if(!medicalRecord.isPresent())return null;

        List<MedicalHistory> medicalHistories=medicalHistoryRepository.findMedicalHistoryByMedicalRecord(medicalRecord.get());

        return medicalHistoryMapper.toDto(medicalHistories);
    }


}
