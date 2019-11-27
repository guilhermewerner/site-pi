using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using SitePi.Models;

namespace SitePi.Models
{
    public class User
    {
        [Key]
        public int Id { get; set; }

        [Required(ErrorMessage = "This field is required.")]
        [RegularExpression("^[a-zA-Z0-9-_]*$", ErrorMessage = "That value is not allowed.")]
        [StringLength(25, MinimumLength = 5, ErrorMessage = "The user name must be at least 5 characters long.")]
        public string Name { get; set; }

        [Required(ErrorMessage = "This field is required.")]
        [MaxLength(100)]
        [RegularExpression(@"^([a-z0-9_\.\+-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})$", ErrorMessage = "Please enter a valid email address.")]
        [DataType(DataType.EmailAddress)]
        public string Email { get; set; }

        [Required(ErrorMessage = "This field is required.")]
        [StringLength(45, MinimumLength = 8, ErrorMessage = "The password must be at least 8 characters long.")]
        public string PasswordHash { get; set; }

        [NotMapped]
        [Required(ErrorMessage = "This field is required.")]
        [Compare("PasswordHash", ErrorMessage = "The passwords do not match.")]
        public string PasswordConfirm { get; set; }
    }
}
