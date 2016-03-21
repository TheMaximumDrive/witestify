FROM centos:latest
MAINTAINER Wen Chao Chen <mail@wenchaochen.at>

RUN yum -y install httpd; yum clean all
RUN yum -y install php; yum clean all
RUN yum -y install git; yum clean all

RUN yum -y install python-setuptools
RUN easy_install supervisor

RUN git clone https://github.com/TheMaximumDrive/witestify-server.git /var/www/html
RUN git clone https://github.com/TheMaximumDrive/witestify-mysql.git /home

ADD supervisord.conf /etc/supervisord.conf

RUN sed -i 's/AllowOverride None/AllowOverride All/' /etc/httpd/conf/httpd.conf

EXPOSE 22 80

CMD ["/usr/bin/supervisord"]