# Resume Active Activity on Startup

When the app starts, if there is an activity that is started but not completed (startTime != null,
completedAt == null), open it in the compact activity view immediately. The user should see their
in-progress work without having to find it in the agenda.
