package com.unir.employees.controller;

import com.unir.employees.data.DepartmentRepository;
import com.unir.employees.model.db.Department;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    /**
     * Obtener un departamento por nombre.
     *
     * @param deptName - nombre del departamento.
     * @return departamento.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Department> getDepartmentByName(@PathVariable("name") String deptName) {
        return ResponseEntity.ok(departmentRepository.findByDeptName(deptName).orElse(null));
    }

    /**
     * Obtener un departamento por codigo de departamento
     *
     * @param deptNo
     * @return
     */
    @GetMapping("/{deptNo}")
    public ResponseEntity<Department> getDepartmentByNumber(@PathVariable("deptNo") String deptNo) {
        return ResponseEntity.ok(departmentRepository.findByDeptNo(deptNo).orElse(null));
    }

    /**
     * Busca departamentos por numero o nombre
     *
     * @param deptNo
     * @param deptName
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<List<Department>> searchDepartment(
            @RequestParam(name = "deptNo", required = false) String deptNo,
            @RequestParam(name = "deptName", required = false) String deptName
    ) {
        if (deptNo == null && deptName == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(departmentRepository.findByDeptNameOrDeptNo(deptName, deptNo));
    }

    /**
     * Crear un nuevo departamento.
     * <p>
     * Observa que el metodo createDepartmentTransact está anotado con @Transactional.
     * Cuando se llama a un metodo anotado con @Transactional dentro de la misma clase, la anotación no tiene efecto.
     * No se crea, por tanto, un nuevo contexto de transacción, sino que se reutiliza el contexto de transacción actual.
     * Dado que el metodo createDepartmentTransact es llamado desde el metodo createDepartment, es necesario que
     * la anotación @Transactional esté presente en el metodo createDepartment. De hecho, si la retiras, verás un
     * warning en la consola referente a esto.
     *
     * @param department - departamento.
     * @return departamento creado.
     */
    @PostMapping("/")
    @Transactional
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(createDepartmentTransact(department));
    }


    /**
     * Crear un departamento.
     * Si el número de departamentos supera los 13, se lanza una excepción.
     * En este caso, se hace un rollback de la transacción.
     * Si no se produce ninguna excepción, se devuelve el departamento creado.
     * <p>
     * La operación está anotada con @Transactional, lo que significa que se ejecuta en una transacción.
     *
     * @param department - departamento.
     * @return departamento creado.
     * @throws IllegalStateException - excepción lanzada si el número de departamentos supera los 13.
     */
    @Transactional
    protected Department createDepartmentTransact(Department department) throws IllegalStateException {

        Department created = departmentRepository.save(department);
        departmentRepository.findByDeptName(department.getDeptName())
                .ifPresent(
                        recentlyCreated -> log.info("En el contexto de esta conexion, el departamento {} ha sido creado",
                                recentlyCreated.getDeptName()));

        long count = departmentRepository.count();
        if (count >= 13) {
            throw new IllegalStateException("No puede haber más de 13 departamentos. Haciendo rollback...");
        }
        return created;
    }
}
