package com.ecommerce.auth.repository;

import com.ecommerce.auth.model.AppRole;
import com.ecommerce.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    Optional<Role> findByRoleName(AppRole roleName);
    
    Boolean existsByRoleName(AppRole roleName);
} 