FROM rclone/rclone:1.75.1

COPY backup.sh /usr/local/bin/saltos-r2-backup
RUN chmod 0755 /usr/local/bin/saltos-r2-backup

ENTRYPOINT ["/bin/sh", "/usr/local/bin/saltos-r2-backup"]
