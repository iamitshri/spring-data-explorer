package com.example.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
@Order(2)
@RequiredArgsConstructor
public class ManyToManyExampleRunner implements ApplicationRunner {

	final PermissionsRepo permissionRepo;

	final RoleRepo roleRepo;

	final UserRepo userRepo;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		permissionRepo.save(new Permissions("sys", "admin"));
		permissionRepo.save(new Permissions("admin-removeusers", "admin"));
		permissionRepo.save(new Permissions("admin-updateusers", "admin"));

		permissionRepo.save(new Permissions("evaluator-save", "evaluator"));
		permissionRepo.save(new Permissions("evaluator-cancel", "evaluator"));
		permissionRepo.save(new Permissions("publisher-update", "publisher"));
		permissionRepo.save(new Permissions("publisher-add", "publisher"));

		roleRepo.save(new Role("evaluator"));
		roleRepo.save(new Role("admin"));
		roleRepo.save(new Role("publisher"));

		// add a permission to the role
		Optional<Role> role = roleRepo.findById(8L);
		role.get().getPermissions().add(permissionRepo.findById(1L).get());
		roleRepo.save(role.get());

		Role adminRole = roleRepo.findByRoleType("admin");
		adminRole.getPermissions().addAll(permissionRepo.findAll());
		roleRepo.save(adminRole);

		log.info("Done adding all the roles and permissions ");

		User amit = userRepo.save(new User("Amit"));

		List<Role> roles = new ArrayList<>();
		roles.add(role.get());
		roles.add(roleRepo.findByRoleType("admin"));
		amit.setRoles(roles);
		userRepo.save(amit);

		RoleProjection result = roleRepo.findByRoleId(8L);
		log.debug("Projection feature test:{}, {}", result.getCreatedDate(), result.getLastModifiedDate());
		
		result.getPermissions().forEach(p -> log.debug(p.toString()));

		User useramit = userRepo.findByName("amit");
		log.debug("User:{}", useramit.getPermissions());

		List<Permissions> perms = userRepo.getUserPermissions(useramit.getUserId());
		log.debug("get User permission using jpa query:{}", perms);
	}

}

@RestController
class RoleController {

	@Autowired
	PermissionsRepo permissionRepo;

	@Autowired
	RoleRepo roleRepo;

	@GetMapping("/role/{roletype}")
	ResponseEntity<Role> updateRole(@PathVariable String roletype) {
		return ResponseEntity.ok(roleRepo.findByRoleType(roletype));
	}

	@PostMapping("/role/{roletype}/permission/{permissionId}")
	ResponseEntity<Role> addPermissionToRole(@PathVariable String roletype, @PathVariable Long permissionId) {
		Role r = roleRepo.findByRoleType(roletype);
		r.getPermissions().add(new Permissions(permissionId));
		return ResponseEntity.ok(roleRepo.save(r));
	}

	@PostMapping("/role/{roletype}/permissions")
	ResponseEntity<Role> addPermissionsToRole(@PathVariable String roletype, @RequestBody Long[] permissions) {
		Role r = roleRepo.findByRoleType(roletype);
		List<Permissions> list = new ArrayList<>();
		Arrays.asList(permissions).forEach(permissionId -> list.add(new Permissions(permissionId)));
		r.setPermissions(list);
		return ResponseEntity.ok(roleRepo.save(r));
	}

	@GetMapping("/roles")
	ResponseEntity<List<Role>> getAllRoles() {
		return ResponseEntity.ok(roleRepo.findAll());
	}

	@PutMapping("/roles")
	ResponseEntity<List<Role>> addRoles(@RequestBody ArrayList<Role> roles) {

		return ResponseEntity.ok(roleRepo.saveAll(roles));
	}

	@GetMapping("/permissions")
	ResponseEntity<List<Permissions>> getAllPermissions() {
		return ResponseEntity.ok(permissionRepo.findAll());
	}

	@PutMapping("/permissions")
	ResponseEntity<List<Permissions>> addPermissions(@RequestBody ArrayList<Permissions> permissions) {
		return ResponseEntity.ok(permissionRepo.saveAll(permissions));
	}
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
class User {
	@Id
	@GeneratedValue
	Long userId;

	@NonNull
	String name;

	@CreatedDate
	Date createdDate;

	@LastModifiedDate
	Date lastModifiedDate;

	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "userId", referencedColumnName = "userId"), inverseJoinColumns = @JoinColumn(name = "roleId", referencedColumnName = "roleId"))
	List<Role> roles = new ArrayList<>();

	@Transient
	List<Permissions> permissions = new ArrayList<>();

	List<Permissions> getPermissions() {
		roles.stream().forEach(role -> permissions.addAll(role.getPermissions()));
		return permissions;
	}

}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class Role {
	@Id
	@GeneratedValue
	Long roleId;

	@NonNull
	String roleType;

	@CreatedDate
	Date createdDate;

	@LastModifiedDate
	Date lastModifiedDate;

	/**
	 * With cascade type persist we can add new permission when adding the new role
	 * we can not use existing permission id to edit. It gives this error "detached
	 * entity passed to persist:"
	 * 
	 * Editing an existing Role with permissions worked. I was able to edit/update
	 * and add new permission to existing to role
	 * 
	 * but it turned out you can not add existing permission to brand new role (
	 * role w/o id)
	 * 
	 */
	@ManyToMany(cascade = { CascadeType.PERSIST }, fetch = FetchType.EAGER)
	@JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "roleId", referencedColumnName = "roleId"), inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "permission_id"))
	List<Permissions> permissions = new ArrayList<>();

	@ManyToMany(mappedBy = "roles")
	List<User> users = new ArrayList<>();
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class Permissions {
	@Id
	@GeneratedValue
	Long permission_id;

	@NonNull
	String name;

	@NonNull
	String type;

	@CreatedDate
	LocalDateTime createdDate;

	@LastModifiedDate
	LocalDateTime lastModifiedDate;

	Permissions(Long permission_id) {
		this.permission_id = permission_id;
	}

	@ManyToMany(mappedBy = "permissions")
	List<Role> roles = new ArrayList<Role>();

	public String toString() {
		return "Permission[ permissionId:" + permission_id + ", name:" + name + ", type:" + type + "]";
	}
}

interface RoleProjection {
	Date getCreatedDate();

	Date getLastModifiedDate();

	List<PermissionProjection> getPermissions();

}

interface PermissionProjection {
	String getName();
}

interface UserRepo extends JpaRepository<User, Long> {
	User findByName(String name);

	@Query("select r.permissions from User u, Role r where u.userId=:userId and r in (select r1 from u.roles r1)")
	List<Permissions> getUserPermissions(@Param(value = "userId") Long userId);
}

interface RoleRepo extends JpaRepository<Role, Long> {
	Role findByRoleType(String roleType);

	RoleProjection findByRoleId(Long roleId);
}

interface PermissionsRepo extends JpaRepository<Permissions, Long> {

}
