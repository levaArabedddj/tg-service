package org.example.tgservice.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import lombok.RequiredArgsConstructor;
import org.example.tgservice.BD.Feedback;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import org.example.tgservice.BD.Feedback;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = System.getenv("APPLICATION_NAME");
    private static final String SPREADSHEET_ID = System.getenv("SPREADSHEET_ID");
    private static final String CREDENTIALS_FILE_PATH = System.getenv("CREDENTIALS_FILE_PATH");

    private Sheets getSheetsService() throws Exception {
        var credentials = GoogleCredential.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH))
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credentials)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void appendFeedback(Feedback feedback) {
        try {
            Sheets sheetsService = getSheetsService();
            List<Object> row = Arrays.asList(
                    feedback.getChatId(),
                    feedback.getMessage(),
                    feedback.getSentiment(),
                    feedback.getCriticality(),
                    feedback.getSuggestion(),
                    feedback.getCreatedAt().toString()
            );

            ValueRange body = new ValueRange().setValues(List.of(row));
            sheetsService.spreadsheets().values()
                    .append(SPREADSHEET_ID, "Лист1!A:F", body)
                    .setValueInputOption("RAW")
                    .execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

