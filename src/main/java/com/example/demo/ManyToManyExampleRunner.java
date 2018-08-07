package com.example.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(2)
public class ManyToManyExampleRunner implements ApplicationRunner {

    @Autowired
    PermissionsRepo permissionRepo;

    @Autowired
    RoleRepo roleRepo;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        permissionRepo.save(new Permisssions("sys", "admin"));
        permissionRepo.save(new Permisssions("admin-removeusers", "admin"));
        permissionRepo.save(new Permisssions("admin-updateusers", "admin"));

        permissionRepo.save(new Permisssions("evaluator-save", "evaluator"));
        permissionRepo.save(new Permisssions("evaluator-cancel", "evaluator"));
        permissionRepo.save(new Permisssions("publisher-update", "publisher"));
        permissionRepo.save(new Permisssions("publisher-add", "publisher"));

        roleRepo.save(new Role("evaluator"));
        roleRepo.save(new Role("admin"));
        roleRepo.save(new Role("publisher"));

        log.error("tale");
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
    ResponseEntity<Role> addPermissionToRole(@PathVariable String roletype,
            @PathVariable Long permissionId) {
        Role r = roleRepo.findByRoleType(roletype);
        r.getPermissions().add(new Permisssions(permissionId));
        return ResponseEntity.ok(roleRepo.save(r));
    }
    
    @PostMapping("/role/{roletype}/permissions")
    ResponseEntity<Role> addPermissionsToRole(@PathVariable String roletype,
            @RequestBody Long[] permissions) {
        Role r = roleRepo.findByRoleType(roletype);
        Arrays.asList(permissions)
                .forEach(permissionId -> r.getPermissions().add(new Permisssions(permissionId)));
        return ResponseEntity.ok( roleRepo.save(r));
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
    ResponseEntity<List<Permisssions>> getAllPermissions() {
        return ResponseEntity.ok(permissionRepo.findAll());
    }
    
    @PutMapping("/permissions")
    ResponseEntity<List<Permisssions>> addPermissions(@RequestBody ArrayList<Permisssions> permissions) {
        return ResponseEntity.ok(permissionRepo.saveAll(permissions));
    }
}


@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Role {
    @Id
    @GeneratedValue
    Long role_id;

    @NonNull
    String roleType;
    
    /**
     *  With cascade type persist
     *  we can add new permission when adding the new role 
     *  we can not use existing permission id to edit. It gives this error 
     *  "detached entity passed to persist:" 
     *  
     *  Editing an existing Role with permissions worked. I was able to edit/update and add new permission to existing to role
     *  
     *  but it turned out you can not add existing permission to brand new role ( role w/o id)
     *  
     */
    @ManyToMany(cascade = { CascadeType.PERSIST,CascadeType.MERGE} )
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id",
                    referencedColumnName = "permission_id"))
    List<Permisssions> permissions = new  ArrayList<>();
}


@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Permisssions {
    @Id
    @GeneratedValue
    Long permission_id;
    @NonNull
    String name;
    @NonNull
    String type;
    
    Permisssions(Long permission_id){
        this.permission_id = permission_id;
    }
}


interface RoleRepo extends JpaRepository<Role, Long> {
    Role findByRoleType(String roleType);
}


interface PermissionsRepo extends JpaRepository<Permisssions, Long> {

}
