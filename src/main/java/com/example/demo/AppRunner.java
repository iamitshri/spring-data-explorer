package com.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Order(1)
public class AppRunner implements ApplicationRunner {

	@Autowired
	PersonRepository personRepo;

	Random rand = new Random();

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.debug("welcome to the Spring Data Demo ");

		IntStream.range(1, 10).forEach(i -> personRepo.save(new Person("Amit", "Shri", i * rand.nextInt(30),new Teacher("teacher_name_"+rand.nextInt(30)), SexType.MALE)));
	      IntStream.range(1, 10).forEach(i -> personRepo.save(new Person("Monal", "Agni", i * rand.nextInt(30),new Teacher("teacher_name_"+rand.nextInt(30)), SexType.FEMALE)));
	      IntStream.range(1, 2).forEach(i -> personRepo.save(new Person("Cuckoo", "sacredgames", i * rand.nextInt(30),new Teacher("teacher_name_"+rand.nextInt(30)), SexType.TRANS)));

		personRepo.findByFirstName("AMIT", Sort.by(Direction.DESC, "id"))
				.forEach(person -> log.debug(person.toString()));

		personRepo.findByFirstNameAndLastName("amit", "shri").forEach(p -> log.debug(p.toString()));

		personRepo.findByIdLessThan(5L).forEach(p -> log.debug(p.toString()));

		personRepo.personsWithCatsGreaterThan(30).forEach(p -> log.debug(p.toString()));

		personRepo.personsWithCatsLessThan(30).forEach(p -> log.debug(p.toString()));

		personRepo.personsWithFirstNameLike("Amit").forEach(p -> log.debug(p.toString()));
		
		personRepo.findByTeacher_teacherId(2l).forEach(p -> log.debug(p.toString()));
		
		personRepo.findByTeacher_teacherId(8l).forEach(p -> log.debug(p.toString()));
		
		
		List<Long> ids = new ArrayList<>();
		ids.add(2l);
		ids.add(4l);
		ids.add(8l);
		personRepo.findByTeacher_teacherIdIn(ids).forEach(p -> log.debug(p.toString()));
		
		log.debug("trans coming up");
		personRepo.findByStatus(SexType.TRANS).forEach(p -> log.debug(p.toString()));
	}

}

@Entity
@Table(name = "person")
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@NamedQuery(name = "Person.personsWithCatsGreaterThan", query = "select p from Person p where p.numOfCats >= ?1")
class Person {
    @GeneratedValue
    @Id
    Long id;

    @Column
    @NonNull
    String firstName;

    @Column
    @NonNull
    String lastName;

    @Column
    @NonNull
    Integer numOfCats;

    @NonNull
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "teacher_id")
    Teacher teacher;

    @NonNull
    @Column(name = "sex", columnDefinition = "enum('MALE','FEMALE','TRANS')")
    @Enumerated(EnumType.STRING)
    private SexType status;
}

enum SexType{
    MALE, FEMALE, TRANS;
}

@Entity
@Table(name = "teacher")
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
class Teacher {

	@GeneratedValue
	@Id
	Long teacherId;

	@Column
	@NonNull
	String name;

	 

}

interface PersonRepository extends JpaRepository<Person, Long> {
	List<Person> findByFirstName(String name, Sort sort);

	List<Person> findByFirstNameAndLastName(String f, String l);

	List<Person> findByIdLessThan(Long i);

	List<Person> personsWithCatsGreaterThan(Integer i);

	@Query("select p from Person p where p.numOfCats <= ?1")
	List<Person> personsWithCatsLessThan(Integer i);

	@Query(nativeQuery = true, value = "select * from person p where p.first_name like  %:fName")
	List<Person> personsWithFirstNameLike(@Param("fName") String firstName);

	List<Person> findByTeacher_teacherId(Long id);
	List<Person> findByTeacherTeacherId(Long id);
	List<Person> findByTeacher_teacherIdIn(List<Long> idList);
	
	List<Person> findByStatus(SexType type);
}