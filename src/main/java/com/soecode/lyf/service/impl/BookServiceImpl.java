package com.soecode.lyf.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soecode.lyf.dao.AppointmentDao;
import com.soecode.lyf.dao.BookDao;
import com.soecode.lyf.dto.AppointExecution;
import com.soecode.lyf.entity.Appointment;
import com.soecode.lyf.entity.Book;
import com.soecode.lyf.enums.AppointStateEnum;
import com.soecode.lyf.exception.AppointException;
import com.soecode.lyf.exception.NoNumberException;
import com.soecode.lyf.exception.RepeatAppointException;
import com.soecode.lyf.service.BookService;
// 一个接口BookService   一个实现BookServiceImpl
@Service
//@Service用于标注业务层组件
public class BookServiceImpl implements BookService {
// https://blog.csdn.net/level_level/article/details/4248685   DML、DDL、DCL区别
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	// 注入Service依赖
	@Autowired
	private BookDao bookDao;

	@Autowired
	private AppointmentDao appointmentDao;


	@Override
	public Book getById(long bookId) {
		return bookDao.queryById(bookId);
	}

	@Override
	public List<Book> getList() {
		return bookDao.queryAll(0, 1000);
	}

	@Override
	@Transactional
	/**
	 * 使用注解控制事务方法的优点： 1.开发团队达成一致约定，明确标注事务方法的编程风格
	 * 2.保证事务方法的执行时间尽可能短，不要穿插其他网络操作，RPC/HTTP请求或者剥离到事务方法外部
	 * 3.不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
	 */
	public AppointExecution appoint(long bookId, long studentId) {
		try {
			// 减库存
			int update = bookDao.reduceNumber(bookId);
			if (update <= 0) {// 库存不足
				throw new NoNumberException("no number");
			} else {
				// 执行预约操作
				int insert = appointmentDao.insertAppointment(bookId, studentId);
				if (insert <= 0) {// 重复预约
					throw new RepeatAppointException("repeat appoint");
				} else {// 预约成功
					Appointment appointment = appointmentDao.queryByKeyWithBook(bookId, studentId);
// https://www.cnblogs.com/feng9exe/p/5611992.html		
// 什么是领域模型(domain model)？贫血模型(anaemic domain model) 和充血模型(rich domain model)有什么区别
// 从上图还可以看到，表现层与应用层之间是通过数据传输对象（DTO）进行交互的，数据传输对象是没有行为的POCO对象，
// 	它的目的只是为了对领域对象进行数据封装，实现层与层之间的数据传递。为何不能直接将领域对象用于数据传递？
// 因为领域对象更注重领域，而DTO更注重数据。不仅如此，由于“富领域模型”的特点，这样做会直接将领域对象的行为暴露给表现层
					return new AppointExecution(bookId, AppointStateEnum.SUCCESS, appointment);
				}
			}
		} catch (NoNumberException e1) {
			throw e1;
		} catch (RepeatAppointException e2) {
			throw e2;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// 所有编译期异常转换为运行期异常
			throw new AppointException("appoint inner error:" + e.getMessage());
		}
	}

}
