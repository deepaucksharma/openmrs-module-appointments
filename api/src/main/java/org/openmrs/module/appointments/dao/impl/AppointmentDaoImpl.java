package org.openmrs.module.appointments.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.openmrs.module.appointments.dao.AppointmentDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentService;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class AppointmentDaoImpl implements AppointmentDao {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public List<Appointment> getAllAppointments(Date forDate) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Appointment.class);
        criteria.add(Restrictions.eq("voided", false));
        if (forDate != null) {
            Date maxDate = new Date(forDate.getTime() + TimeUnit.DAYS.toMillis(1));
            criteria.add(Restrictions.ge("startDateTime", forDate));
            criteria.add(Restrictions.lt("endDateTime", maxDate));
        }
        return criteria.list();
    }

    @Transactional
    @Override
    public void save(Appointment appointment) {

        if(appointment.getAppointmentNumber() == null) {
            appointment.setAppointmentNumber(generateAppointmentNumber());
        }
        sessionFactory.getCurrentSession().saveOrUpdate(appointment);
    }

    private String generateAppointmentNumber() {
        // placeholder for generating appointment numbers
        return "0000";
    }

    @Override
    public List<Appointment> search(Appointment appointment) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Appointment.class).add(
                Example.create(appointment).excludeProperty("uuid"));

        if(appointment.getPatient()!=null) criteria.createCriteria("patient").add(
                Example.create(appointment.getPatient()));

        if(appointment.getLocation()!=null) criteria.createCriteria("location").add(
                Example.create(appointment.getLocation()));

        if(appointment.getService()!=null) criteria.createCriteria("service").add(
                Example.create(appointment.getService()));

        if(appointment.getProvider()!=null) criteria.createCriteria("provider").add(
                Example.create(appointment.getProvider()));

        return criteria.list();
    }

    @Override
    public List<Appointment> getAllFutureAppointmentsForService(AppointmentService appointmentService) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Appointment.class);
        criteria.add(Restrictions.eq("service", appointmentService));
        criteria.add(Restrictions.gt("endDateTime", new Date()));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.ne("status", AppointmentStatus.Cancelled));
        return criteria.list();
    }

    @Override
    public List<Appointment> getAllFutureAppointmentsForServiceType(AppointmentServiceType appointmentServiceType) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Appointment.class);
        criteria.add(Restrictions.eq("serviceType", appointmentServiceType));
        criteria.add(Restrictions.gt("endDateTime", new Date()));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.ne("status", AppointmentStatus.Cancelled));
        return criteria.list();
    }

    @Override
    public List<Appointment> getAppointmentsForService(AppointmentService appointmentService, Date startDate, Date endDate, List<AppointmentStatus> appointmentStatusFilterList) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Appointment.class);
        criteria.createAlias("serviceType", "serviceType", JoinType.LEFT_OUTER_JOIN);
        criteria.add(Restrictions.or(Restrictions.isNull("serviceType"), Restrictions.eq("serviceType.voided", false)));
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.ge("startDateTime", startDate));
        criteria.add(Restrictions.le("startDateTime", endDate));
        criteria.createCriteria("service").add(Example.create(appointmentService));
        if (appointmentStatusFilterList != null && !appointmentStatusFilterList.isEmpty()) {
            criteria.add(Restrictions.in("status", appointmentStatusFilterList));
        }
        return  criteria.list();

    }
}
