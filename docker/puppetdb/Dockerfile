FROM ubuntu:16.04

ARG vcs_ref
ARG build_date
ARG version="6.0.0"

ENV PUPPETDB_VERSION="$version"
ENV DUMB_INIT_VERSION="1.2.1"
ENV UBUNTU_CODENAME="xenial"
ENV PUPPETDB_DATABASE_CONNECTION="//postgres:5432/puppetdb"
ENV PUPPETDB_USER=puppetdb
ENV PUPPETDB_PASSWORD=puppetdb
ENV PUPPETDB_NODE_TTL=7d
ENV PUPPETDB_NODE_PURGE_TTL=14d
ENV PUPPETDB_REPORT_TTL=14d
ENV PUPPETDB_JAVA_ARGS="-Djava.net.preferIPv4Stack=true -Xms256m -Xmx256m"
# used by entrypoint to determine if puppetserver should be contacted for config
# set to false when container tests are run
ENV USE_PUPPETSERVER=true
ENV PATH="/opt/puppetlabs/server/bin:/opt/puppetlabs/puppet/bin:/opt/puppetlabs/bin:$PATH"

LABEL org.label-schema.maintainer="Puppet Release Team <release@puppet.com>" \
      org.label-schema.vendor="Puppet" \
      org.label-schema.url="https://github.com/puppetlabs/puppetdb" \
      org.label-schema.name="PuppetDB" \
      org.label-schema.license="Apache-2.0" \
      org.label-schema.version="$PUPPETDB_VERSION" \
      org.label-schema.vcs-url="https://github.com/puppetlabs/puppetdb" \
      org.label-schema.vcs-ref="$vcs_ref" \
      org.label-schema.build-date="$build_date" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.dockerfile="/Dockerfile"

RUN apt-get update && \
    apt-get install --no-install-recommends -y wget netcat lsb-release ca-certificates && \
    wget https://apt.puppetlabs.com/puppet6-release-"$UBUNTU_CODENAME".deb && \
    wget https://github.com/Yelp/dumb-init/releases/download/v"$DUMB_INIT_VERSION"/dumb-init_"$DUMB_INIT_VERSION"_amd64.deb && \
    dpkg -i puppet6-release-"$UBUNTU_CODENAME".deb && \
    dpkg -i dumb-init_"$DUMB_INIT_VERSION"_amd64.deb && \
    rm puppet6-release-"$UBUNTU_CODENAME".deb dumb-init_"$DUMB_INIT_VERSION"_amd64.deb && \
    apt-get update && \
    apt-get install --no-install-recommends -y puppet-agent puppetdb="$PUPPETDB_VERSION"-1"$UBUNTU_CODENAME" && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY puppetdb /etc/default/
COPY logging /etc/puppetlabs/puppetdb/logging

RUN rm -fr /etc/puppetlabs/puppetdb/conf.d
COPY conf.d /etc/puppetlabs/puppetdb/conf.d

# Persist the agent SSL certificate.
VOLUME /etc/puppetlabs/puppet/ssl/
# /etc/puppetlabs/puppetdb/ssl is automatically populated from here and
# doesn't need a separate volume.

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080 8081

ENTRYPOINT ["dumb-init", "/docker-entrypoint.sh"]
CMD ["foreground"]

# The start-period is just a wild guess how long it takes PuppetDB to come
# up in the worst case. The other timing parameters are set so that it
# takes at most a minute to realize that PuppetDB has failed.
HEALTHCHECK --interval=10s --timeout=10s --retries=6 \
  --start-period=5m CMD \
  curl --fail --silent \
  --resolve 'puppetdb:8080:127.0.0.1' \
  http://puppetdb:8080/status/v1/services/puppetdb-status \
  | grep -q '"state":"running"' \
  || exit 1

COPY Dockerfile /
