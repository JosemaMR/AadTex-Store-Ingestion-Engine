package service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import repository.CsvReader;
import repository.TxtReader;

import java.io.File;

@Repository
@RequiredArgsConstructor
public class ProcessFileService {

    private final CsvReader csvReader;
    private final TxtReader txtReader;



    // aqui es donde hago todas las cuentas e historias


    public void taltaltal(){
        File a = csvReader.read();
        File b = txtReader.read();
        mergeFileProcess();
    }
}
