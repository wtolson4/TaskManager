from sqlite_utils import Database
import argparse
import os

parser = argparse.ArgumentParser(
                    description="Convert a Regularly DB to this program's format")
parser.add_argument('infile', metavar='<path to regularly.db>')
parser.add_argument('outdir', metavar='<directory for output>')
args = parser.parse_args()

old_db = Database(args.infile)
new_db = Database(os.path.join(os.path.dirname(args.outdir), "converted.db"), recreate=True)

# From inspection, need this table for Android
new_db.execute("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)")
new_db.execute("INSERT OR REPLACE INTO android_metadata (locale) VALUES('en_US')")

# Copy this from the "schemas/1.json"
new_db.execute('''
    CREATE TABLE `tasks_table` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `description` TEXT NOT NULL,
    `creationDate` TEXT NOT NULL,
    `initialDueDate` TEXT NOT NULL,
    `period` INTEGER NOT NULL,
    `notificationsEnabled` INTEGER NOT NULL,
    `notificationLastDismissed` TEXT,
    `notificationTime` TEXT,
    `notificationPeriod` INTEGER)
''')
new_db.execute('''
    CREATE TABLE completion_date_table (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `taskId` INTEGER NOT NULL,
    `date` TEXT NOT NULL,
    `note` TEXT)
''')
new_db.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
new_db.execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c37f072b888bfa933f5d70598636d779')")

old_id_to_new = {}

for row in old_db["tasks"].rows:
    new_id = new_db["tasks_table"].insert({
        'id': None, # To autogenerate
        'name': row['name'],
        'description': row['details'],
        'creationDate': row['created'],
        'initialDueDate': row['firstdue'] or row['created'], # This should really be "row['created'] + row['period']", but it's rare enough that it's not worth worrying about
        'period': row['period'],
        'notificationsEnabled': row['notifications_enabled'],
        'notificationLastDismissed': row['lastnotified'] + "T00:00:00" if row['lastnotified'] else None,
        'notificationTime': row['notifications_time'] + ":00" if row['notifications_time'] else None,
        'notificationPeriod': row['notifications_period']
    }).last_pk
    old_id_to_new[row['_id']] = new_id

for row in old_db["log"].rows:
    new_db["completion_date_table"].insert({
        'id': None, # To autogenerate
        'taskId': old_id_to_new[row['taskid']],
        'date': row['entrydate'],
        'note': row['note'],
    })