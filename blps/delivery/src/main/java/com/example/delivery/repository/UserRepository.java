package com.example.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);

	List<User> findByRole(UserRole role);

	Page<User> findAllByOrderByIdAsc(Pageable pageable);

	Page<User> findByRoleOrderByIdAsc(UserRole role, Pageable pageable);

	Optional<User> findFirstByRoleAndAvailableTrueOrderByIdAsc(UserRole role);

	Optional<User> findFirstByRoleOrderByIdAsc(UserRole role);

	Optional<User> findFirstByRoleAndUsernameIsNullOrderByIdAsc(UserRole role);
}
