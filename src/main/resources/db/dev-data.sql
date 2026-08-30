INSERT INTO app_users (username)
VALUES ('alice'),
       ('bob'),
       ('carol'),
       ('dave'),
       ('erin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO tasks (title, description, status, category, user_id)
SELECT seed.title,
       seed.description,
       seed.status,
       seed.category,
       app_users.id
FROM (VALUES
          ('Plan weekly meals', 'Choose recipes and prepare a grocery list.', 'TODO', 'HOME', 'alice'),
          ('Organize the garage', 'Sort tools and donate unused equipment.', 'IN_PROGRESS', 'HOME', 'bob'),
          ('Repair kitchen faucet', 'Replace the worn faucet cartridge.', 'DONE', 'HOME', 'carol'),
          ('Clean apartment windows', 'Wash all interior and exterior windows.', 'TODO', 'HOME', 'dave'),
          ('Prepare sprint review', 'Summarize completed work and open risks.', 'IN_PROGRESS', 'WORK', 'alice'),
          ('Update API documentation', 'Document task filtering and error responses.', 'DONE', 'WORK', 'bob'),
          ('Review pull requests', 'Review the pending backend changes.', 'TODO', 'WORK', 'carol'),
          ('Draft quarterly goals', 'Define measurable goals for the next quarter.', 'IN_PROGRESS', 'WORK', 'dave'),
          ('Archive old project files', 'Move completed project files into the archive.', 'DONE', 'WORK', 'erin'),
          ('Study Spring Security', 'Complete the authentication fundamentals module.', 'TODO', 'EDUCATION', 'alice'),
          ('Practice PostgreSQL indexes', 'Compare query plans before and after indexing.', 'IN_PROGRESS', 'EDUCATION', 'bob'),
          ('Read Java concurrency chapter', 'Take notes on virtual threads and executors.', 'DONE', 'EDUCATION', 'carol'),
          ('Build a caching demo', 'Compare cached and uncached response times.', 'TODO', 'EDUCATION', 'dave'),
          ('Complete Liquibase tutorial', 'Practice rollback and context-based changes.', 'IN_PROGRESS', 'EDUCATION', 'erin'),
          ('Book dentist appointment', 'Schedule the next routine dental checkup.', 'DONE', 'OTHER', 'alice'),
          ('Renew library membership', 'Update contact information and renew the card.', 'TODO', 'OTHER', 'bob'),
          ('Plan weekend hike', 'Select a trail and check required equipment.', 'IN_PROGRESS', 'OTHER', 'carol'),
          ('Back up personal photos', 'Copy this year''s photos to encrypted storage.', 'DONE', 'OTHER', 'dave'),
          ('Buy birthday gift', 'Choose and order a gift before Friday.', 'TODO', 'OTHER', 'erin'),
          ('Schedule car maintenance', 'Arrange an oil change and brake inspection.', 'IN_PROGRESS', 'OTHER', 'erin')
     ) AS seed(title, description, status, category, username)
JOIN app_users ON app_users.username = seed.username
WHERE NOT EXISTS (
    SELECT 1
    FROM tasks existing_task
    WHERE existing_task.title = seed.title
);
