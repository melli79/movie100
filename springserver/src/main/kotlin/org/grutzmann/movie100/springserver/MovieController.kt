package org.grutzmann.movie100.springserver

import de.gruetzmann.movie100.model.ErrorDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class MovieController(private val service :MovieService) {

    @GetMapping("/search")
    fun search(@RequestParam("q", required = true) query :String) :ResponseEntity<*> {
        return if (query.isBlank())
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto("Missing query parameter"))
        else {
            val candidates = service.searchMovie(query)
            ResponseEntity.status(HttpStatus.OK).body(candidates)
        }
    }
}
