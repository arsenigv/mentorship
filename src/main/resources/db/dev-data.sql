INSERT INTO app_users (username)
VALUES ('alice'),
       ('bob'),
       ('carol'),
       ('dave'),
       ('erin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO tasks (title, description, status, due_date, category, user_id)
SELECT seed.title,
       seed.description,
       seed.status,
       CURRENT_DATE + seed.days_until_due,
       seed.category,
       app_users.id
FROM (VALUES
          ('Plan weekly meals', 'Choose recipes and prepare a grocery list.', 'TODO', 1, 'HOME', 'alice'),
          ('Organize the garage', 'Sort tools and donate unused equipment.', 'IN_PROGRESS', 2, 'HOME', 'bob'),
          ('Repair kitchen faucet', 'Replace the worn faucet cartridge.', 'DONE', -1, 'HOME', 'carol'),
          ('Clean apartment windows', 'Wash all interior and exterior windows.', 'TODO', 3, 'HOME', 'dave'),
          ('Prepare sprint review', 'Summarize completed work and open risks.', 'IN_PROGRESS', 4, 'WORK', 'alice'),
          ('Update API documentation', 'Document task filtering and error responses.', 'DONE', -2, 'WORK', 'bob'),
          ('Review pull requests', 'Review the pending backend changes.', 'TODO', 5, 'WORK', 'carol'),
          ('Draft quarterly goals', 'Define measurable goals for the next quarter.', 'IN_PROGRESS', 6, 'WORK', 'dave'),
          ('Archive old project files', 'Move completed project files into the archive.', 'DONE', -3, 'WORK', 'erin'),
          ('Study Spring Security', 'Complete the authentication fundamentals module.', 'TODO', 7, 'EDUCATION', 'alice'),
          ('Practice PostgreSQL indexes', 'Compare query plans before and after indexing.', 'IN_PROGRESS', 8, 'EDUCATION', 'bob'),
          ('Read Java concurrency chapter', 'Take notes on virtual threads and executors.', 'DONE', -4, 'EDUCATION', 'carol'),
          ('Build a caching demo', 'Compare cached and uncached response times.', 'TODO', 9, 'EDUCATION', 'dave'),
          ('Complete Liquibase tutorial', 'Practice rollback and context-based changes.', 'IN_PROGRESS', 10, 'EDUCATION', 'erin'),
          ('Book dentist appointment', 'Schedule the next routine dental checkup.', 'DONE', -5, 'OTHER', 'alice'),
          ('Renew library membership', 'Update contact information and renew the card.', 'TODO', 11, 'OTHER', 'bob'),
          ('Plan weekend hike', 'Select a trail and check required equipment.', 'IN_PROGRESS', 12, 'OTHER', 'carol'),
          ('Back up personal photos', 'Copy this year''s photos to encrypted storage.', 'DONE', -6, 'OTHER', 'dave'),
          ('Buy birthday gift', 'Choose and order a gift before Friday.', 'TODO', 13, 'OTHER', 'erin'),
          ('Schedule car maintenance', 'Arrange an oil change and brake inspection.', 'IN_PROGRESS', 14, 'OTHER', 'erin')
     ) AS seed(title, description, status, days_until_due, category, username)
JOIN app_users ON app_users.username = seed.username
WHERE NOT EXISTS (
    SELECT 1
    FROM tasks existing_task
    WHERE existing_task.title = seed.title
);
