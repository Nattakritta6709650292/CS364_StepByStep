
package com.example.stepbystep;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.stepbystep.database.ActivityRepository;
import com.example.stepbystep.session.SessionData;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DatabaseInstrumentedTest {

    @Test
    public void testSaveAndReadSession() {

        Context context =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();

        ActivityRepository repository =
                new ActivityRepository(context);

        SessionData data = new SessionData(
                150,
                20,
                60,
                "2026-11-11 13:40:00"
        );

        long id = repository.insertActivity(data);

        assertTrue(id > 0);

        List<SessionData> history =
                repository.getAllActivities();

        assertNotNull(history);

        boolean found = false;

        for (SessionData session : history) {
            if (session.steps == 150
                    && session.shakes == 20
                    && session.duration == 60
                    && "2026-11-11 13:40:00"
                    .equals(session.dateTime)) {
                found = true;
                break;
            }
        }

        assertTrue("Saved session not found", found);
    }
}
